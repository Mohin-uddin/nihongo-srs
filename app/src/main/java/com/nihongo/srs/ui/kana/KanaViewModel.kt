package com.nihongo.srs.ui.kana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nihongo.srs.NihongoApp
import com.nihongo.srs.data.Kana
import com.nihongo.srs.data.KanaData
import com.nihongo.srs.data.ProgressRepository
import com.nihongo.srs.speech.JapaneseSpeaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KanaQuestion(val kana: Kana, val options: List<String>, val isRetry: Boolean = false)

data class KanaQuizState(
    val questions: List<KanaQuestion> = emptyList(),
    val index: Int = 0,
    val selected: String? = null,
    val total: Int = 0,
    val firstTryCorrect: Int = 0,
) {
    val current: KanaQuestion? get() = questions.getOrNull(index)
    val isFinished: Boolean get() = questions.isNotEmpty() && index >= questions.size
    val percent: Int get() = if (total == 0) 0 else firstTryCorrect * 100 / total
}

/**
 * Hiragana / Katakana chart with sound, plus a 20-question kana quiz.
 * Knowing kana well means you can read the reading of every word instead of romaji.
 */
class KanaViewModel(
    private val speaker: JapaneseSpeaker,
    private val progress: ProgressRepository,
) : ViewModel() {

    val katakana = MutableStateFlow(false)

    private val _quiz = MutableStateFlow<KanaQuizState?>(null)
    /** null = showing the chart, not a quiz. */
    val quiz: StateFlow<KanaQuizState?> = _quiz.asStateFlow()

    val ttsStatus = speaker.status

    fun speak(kana: Kana) = speaker.speak(kana.char, slow = true)

    fun startQuiz() {
        val all = KanaData.all(katakana.value)
        val picks = all.shuffled().take(QUIZ_SIZE)
        _quiz.value = KanaQuizState(questions = picks.map { question(it, all) }, total = picks.size)
    }

    fun closeQuiz() {
        _quiz.value = null
    }

    private fun question(k: Kana, all: List<Kana>, retry: Boolean = false): KanaQuestion {
        val wrong = all.map { it.romaji }.distinct().filter { it != k.romaji }.shuffled().take(3)
        return KanaQuestion(k, (wrong + k.romaji).shuffled(), retry)
    }

    fun select(option: String) {
        val s = _quiz.value ?: return
        val q = s.current ?: return
        if (s.selected != null) return
        val correct = option == q.kana.romaji
        speaker.speak(q.kana.char, slow = true)
        _quiz.update {
            it?.copy(
                selected = option,
                firstTryCorrect = it.firstTryCorrect + if (correct && !q.isRetry) 1 else 0,
                // Wrong -> this kana comes back at the end.
                questions = if (correct) it.questions
                else it.questions + question(q.kana, KanaData.all(katakana.value), retry = true),
            )
        }
    }

    fun next() {
        _quiz.update { it?.copy(index = it.index + 1, selected = null) }
        val s = _quiz.value ?: return
        if (s.isFinished) viewModelScope.launch { progress.saveExam("KANA", s.percent, s.total) }
    }

    companion object {
        const val QUIZ_SIZE = 20

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NihongoApp
                KanaViewModel(app.speaker, app.progress)
            }
        }
    }
}
