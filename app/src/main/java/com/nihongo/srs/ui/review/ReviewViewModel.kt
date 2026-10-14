package com.nihongo.srs.ui.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nihongo.srs.NihongoApp
import com.nihongo.srs.data.PlayerStore
import com.nihongo.srs.data.Points
import com.nihongo.srs.data.UserProgress
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.data.VocabRepository
import com.nihongo.srs.speech.JapaneseSpeaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** REVIEW = today's due cards. LEARN = new words (Day 0). */
enum class SessionMode { REVIEW, LEARN }

data class CardItem(
    val vocab: VocabEntity,
    val progress: UserProgress?,
    /**
     * Reverse card: show the English/Bangla meaning and ask the user to recall
     * the Japanese. Recalling (not just recognising) is what makes words stick.
     */
    val reverse: Boolean = false,
)

/** One answered card, kept so the user can go back and undo a mis-tap. */
data class DoneStep(
    val index: Int,
    /** LEARN: true if "Learned" was pressed (false = skipped). REVIEW: the answer given. */
    val positive: Boolean,
    val points: Int,
)

data class ReviewUiState(
    val mode: SessionMode = SessionMode.REVIEW,
    val isLoading: Boolean = true,
    val cards: List<CardItem> = emptyList(),
    val index: Int = 0,
    val isFlipped: Boolean = false,
    val correctCount: Int = 0,
    val forgotCount: Int = 0,
    val isBusy: Boolean = false,
    val history: List<DoneStep> = emptyList(),
) {
    val canGoBack: Boolean get() = history.isNotEmpty() && !isBusy
    val current: CardItem? get() = cards.getOrNull(index)
    val isFinished: Boolean get() = !isLoading && index >= cards.size
    val total: Int get() = cards.size
}

/**
 * Holds the state of one flashcard session.
 * Survives screen rotation because it is a ViewModel.
 */
class ReviewViewModel(
    private val repo: VocabRepository,
    private val speaker: JapaneseSpeaker,
    mode: SessionMode,
    private val level: String,
    private val players: PlayerStore,
) : ViewModel() {

    // Learn mode shows the whole card at once: there is nothing to test yet.
    private val showAnswerByDefault = mode == SessionMode.LEARN

    private val _state = MutableStateFlow(
        ReviewUiState(mode = mode, isFlipped = showAnswerByDefault)
    )
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    val ttsStatus = speaker.status

    init {
        load()
    }

    private fun load() = viewModelScope.launch {
        val cards = when (_state.value.mode) {
            SessionMode.REVIEW -> repo.getDueReviews().map {
                // Alternate direction: about half the cards ask for the Japanese.
                val stage = it.progress?.stage ?: 0
                CardItem(it.vocab, it.progress, reverse = (it.vocab.id + stage) % 2L == 1L)
            }
            SessionMode.LEARN -> repo.getNewWords(level, NEW_WORDS_PER_SESSION).map { CardItem(it, null) }
        }
        _state.update { it.copy(isLoading = false, cards = cards, index = 0, isFlipped = showAnswerByDefault) }
        if (showAnswerByDefault) speakCurrent()
    }

    /** Tap on the card: show / hide the reading and meaning. */
    fun flip() {
        if (showAnswerByDefault) return
        _state.update { it.copy(isFlipped = !it.isFlipped) }
        // Auto-play pronunciation when the answer is revealed.
        if (_state.value.isFlipped) speakCurrent()
    }

    fun speakCurrent(slow: Boolean = false) {
        val v = _state.value.current?.vocab ?: return
        // Speak the kana reading so the TTS never guesses the wrong kanji reading.
        speaker.speak(v.reading, slow)
    }

    fun speakExample() {
        val v = _state.value.current?.vocab ?: return
        speaker.speak(v.exampleReading.ifBlank { v.example }.replace(" ", ""), slow = true)
    }

    private fun moveNext(s: ReviewUiState) = s.copy(index = s.index + 1, isFlipped = showAnswerByDefault)

    private fun afterMove() {
        if (showAnswerByDefault) speakCurrent()
    }

    /** REVIEW mode: "I Knew It" (true) or "Forgot" (false). */
    fun answer(correct: Boolean) {
        val s = _state.value
        val card = s.current ?: return
        val progress = card.progress ?: return
        if (s.isBusy) return
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            repo.submitReview(progress, correct)
            players.award(if (correct) Points.REVIEW_CORRECT else Points.REVIEW_TRIED)
            val points = if (correct) Points.REVIEW_CORRECT else Points.REVIEW_TRIED
            _state.update {
                moveNext(it).copy(
                    history = it.history + DoneStep(s.index, correct, points),
                    isBusy = false,
                    correctCount = it.correctCount + if (correct) 1 else 0,
                    forgotCount = it.forgotCount + if (correct) 0 else 1,
                )
            }
        }
    }

    /** LEARN mode: user studied this new word -> Stage 0, first review tomorrow. */
    fun markLearned() {
        val s = _state.value
        val card = s.current ?: return
        if (s.isBusy) return
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            repo.markLearned(card.vocab.id)
            players.award(Points.LEARN)
            _state.update {
                moveNext(it).copy(
                    isBusy = false, correctCount = it.correctCount + 1,
                    history = it.history + DoneStep(s.index, true, Points.LEARN),
                )
            }
            afterMove()
        }
    }

    /** LEARN mode: skip this word for now (it stays in the new-word pool). */
    fun skip() {
        val s = _state.value
        if (s.current == null || s.isBusy) return
        _state.update { moveNext(it).copy(history = it.history + DoneStep(s.index, false, 0)) }
        afterMove()
    }

    /**
     * Back to the previous card and undo what was pressed there, so a mis-tap costs nothing:
     * a "Learned" word becomes new again, a review answer is put back as it was.
     */
    fun goBack() {
        val s = _state.value
        if (!s.canGoBack) return
        val step = s.history.last()
        val card = s.cards.getOrNull(step.index) ?: return
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            when (s.mode) {
                SessionMode.LEARN -> if (step.positive) repo.unlearn(card.vocab.id)
                SessionMode.REVIEW -> card.progress?.let { repo.undoReview(it, step.positive) }
            }
            players.takeBack(step.points)
            _state.update {
                it.copy(
                    index = step.index,
                    isFlipped = showAnswerByDefault,
                    isBusy = false,
                    history = it.history.dropLast(1),
                    correctCount = it.correctCount - if (step.positive) 1 else 0,
                    forgotCount = it.forgotCount - if (it.mode == SessionMode.REVIEW && !step.positive) 1 else 0,
                )
            }
            afterMove()
        }
    }

    override fun onCleared() {
        speaker.stop()
    }

    companion object {
        const val NEW_WORDS_PER_SESSION = 10
        const val ARG_MODE = "mode"
        const val ARG_LEVEL = "level"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NihongoApp
                val handle: SavedStateHandle = createSavedStateHandle()
                val mode = handle.get<String>(ARG_MODE)
                    ?.let { runCatching { SessionMode.valueOf(it) }.getOrNull() }
                    ?: SessionMode.REVIEW
                val level = handle.get<String>(ARG_LEVEL) ?: "N5"
                ReviewViewModel(app.repository, app.speaker, mode, level, app.players)
            }
        }
    }
}
