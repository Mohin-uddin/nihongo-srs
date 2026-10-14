package com.nihongo.srs.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nihongo.srs.NihongoApp
import com.nihongo.srs.data.Player
import com.nihongo.srs.data.PlayerStore
import com.nihongo.srs.data.Points
import com.nihongo.srs.data.VocabRepository
import com.nihongo.srs.speech.JapaneseSpeaker
import com.nihongo.srs.ui.exam.ExamGenerator
import com.nihongo.srs.ui.exam.Question
import com.nihongo.srs.ui.exam.QuestionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Scoreboard and player management. */
class PlayersViewModel(private val store: PlayerStore) : ViewModel() {
    val players: StateFlow<List<Player>> = store.players
    val activeId: StateFlow<Int> = store.activeId

    fun setActive(id: Int) = store.setActive(id)
    fun rename(id: Int, name: String) = store.rename(id, name)
    fun add(name: String) = store.add(name)?.let { store.setActive(it.id) }
    fun remove(id: Int) = store.remove(id)

    companion object {
        val Factory = viewModelFactory {
            initializer { PlayersViewModel((this[APPLICATION_KEY] as NihongoApp).players) }
        }
    }
}

/** One player's side of a quiz battle. */
data class BattleSide(
    val player: Player,
    val score: Int = 0,
    val correct: Int = 0,
    val streak: Int = 0,
)

enum class BattlePhase { SETUP, HANDOFF, QUESTION, FINISHED }

data class BattleState(
    val phase: BattlePhase = BattlePhase.SETUP,
    val players: List<Player> = emptyList(),
    /** The two chosen player ids. */
    val chosen: List<Int> = emptyList(),
    val sides: List<BattleSide> = emptyList(),
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val selected: Int? = null,
    /** Points the last answer earned (for the "+15" pop). */
    val lastGain: Int = 0,
    val isLoading: Boolean = false,
    val notEnoughWords: Boolean = false,
) {
    val turn: Int get() = index % 2
    val current: Question? get() = questions.getOrNull(index)
    val round: Int get() = index / 2 + 1
    val winner: BattleSide?
        get() = sides.takeIf { it.size == 2 && it[0].score != it[1].score }?.maxBy { it.score }
}

/**
 * Quiz battle for two people on one phone: each takes a turn answering the same
 * kind of question, then passes the phone. Right answers give 10 points,
 * 3 in a row adds a bonus, and the winner gets extra points on the scoreboard.
 */
class BattleViewModel(
    private val store: PlayerStore,
    private val repo: VocabRepository,
    private val speaker: JapaneseSpeaker,
) : ViewModel() {

    private val generator = ExamGenerator()

    private val _state = MutableStateFlow(
        BattleState(players = store.players.value, chosen = defaultChosen(store.players.value))
    )
    val state: StateFlow<BattleState> = _state.asStateFlow()

    val ttsStatus = speaker.status

    private fun defaultChosen(list: List<Player>): List<Int> {
        val active = store.activeId.value
        return (listOf(active) + list.map { it.id }.filter { it != active }).take(2)
    }

    fun refreshPlayers() = _state.update {
        val list = store.players.value
        it.copy(players = list, chosen = it.chosen.filter { id -> list.any { p -> p.id == id } }
            .ifEmpty { defaultChosen(list) }.let { c -> if (c.size < 2) defaultChosen(list) else c })
    }

    fun toggleChoose(id: Int) = _state.update {
        val c = when {
            id in it.chosen -> it.chosen - id
            it.chosen.size < 2 -> it.chosen + id
            else -> listOf(it.chosen[1], id)
        }
        it.copy(chosen = c)
    }

    fun start() {
        val s = _state.value
        if (s.chosen.size != 2) return
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val all = repo.getAllWords()
            val learned = repo.getLearned().map { it.vocab }
            // Learned words make it a fair test; with few learned words use beginner words.
            val pool = (if (learned.size >= MIN_POOL) learned else all.filter { it.level == "N5" })
                .filter { it.meaning.isNotBlank() }
            if (pool.size < 4) {
                _state.update { it.copy(isLoading = false, notEnoughWords = true) }
                return@launch
            }
            val picks = pool.shuffled().take(ROUNDS * 2).let { p ->
                // Very small pools repeat words to fill all rounds.
                generateSequence { p }.flatten().take(ROUNDS * 2).toList()
            }
            val types = listOf(QuestionType.JP_TO_MEANING, QuestionType.LISTEN, QuestionType.MEANING_TO_JP)
            // Both players get the same type in a round so it stays fair.
            val questions = picks.mapIndexed { i, w -> generator.question(w, types[(i / 2) % types.size], all) }
            val sides = s.chosen.map { id -> BattleSide(store.players.value.first { it.id == id }) }
            _state.update {
                it.copy(phase = BattlePhase.HANDOFF, sides = sides, questions = questions, index = 0,
                    selected = null, isLoading = false, notEnoughWords = false)
            }
        }
    }

    /** The player holding the phone is ready: show the question. */
    fun ready() {
        _state.update { it.copy(phase = BattlePhase.QUESTION) }
        playIfListening()
    }

    fun select(option: Int) {
        val s = _state.value
        val q = s.current ?: return
        if (s.selected != null) return
        val correct = option == q.correctIndex
        val side = s.sides[s.turn]
        val streak = if (correct) side.streak + 1 else 0
        val gain = if (correct) Points.GAME_CORRECT + if (streak >= 3) Points.GAME_STREAK_BONUS else 0 else 0
        val updated = side.copy(
            score = side.score + gain, correct = side.correct + if (correct) 1 else 0, streak = streak,
        )
        _state.update {
            it.copy(selected = option, lastGain = gain,
                sides = it.sides.mapIndexed { i, b -> if (i == it.turn) updated else b })
        }
        speaker.speak(q.word.reading)
    }

    fun next() {
        val s = _state.value
        val nextIndex = s.index + 1
        if (nextIndex >= s.questions.size) {
            finish()
        } else {
            _state.update { it.copy(index = nextIndex, selected = null, lastGain = 0, phase = BattlePhase.HANDOFF) }
        }
    }

    fun replay() {
        _state.value.current?.let { speaker.speak(it.word.reading) }
    }

    private fun playIfListening() {
        val q = _state.value.current ?: return
        if (q.type == QuestionType.LISTEN) speaker.speak(q.word.reading)
    }

    /** Adds the game to the scoreboard: 2 points per right answer, and a bonus for the winner. */
    private fun finish() {
        val s = _state.value
        s.sides.forEach { store.award(it.correct * 2, it.player.id) }
        s.winner?.let {
            store.award(Points.GAME_WIN, it.player.id)
            store.addWin(it.player.id)
        }
        _state.update { it.copy(phase = BattlePhase.FINISHED, players = store.players.value) }
    }

    fun playAgain() {
        refreshPlayers()
        _state.update { it.copy(phase = BattlePhase.SETUP, sides = emptyList(), questions = emptyList(), index = 0) }
    }

    override fun onCleared() {
        speaker.stop()
    }

    companion object {
        /** Questions per player. */
        const val ROUNDS = 6
        private const val MIN_POOL = 12

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NihongoApp
                BattleViewModel(app.players, app.repository, app.speaker)
            }
        }
    }
}

/** Shown on the battle intro so people know where points come from. */
val pointRules = listOf(
    Triple("Learn a new word", "নতুন শব্দ শেখা", Points.LEARN),
    Triple("Review: knew it", "রিভিউ: মনে ছিল", Points.REVIEW_CORRECT),
    Triple("Review: tried", "রিভিউ: চেষ্টা", Points.REVIEW_TRIED),
    Triple("Exam: right answer", "পরীক্ষা: সঠিক উত্তর", Points.EXAM_CORRECT),
    Triple("Kana quiz: right answer", "কানা কুইজ: সঠিক", Points.KANA_CORRECT),
    Triple("Word saved from news", "খবর থেকে শব্দ সেভ", Points.NEWS_WORD),
    Triple("Win a quiz battle", "কুইজ যুদ্ধে জয়", Points.GAME_WIN),
)
