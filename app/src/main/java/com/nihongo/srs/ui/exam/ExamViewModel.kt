package com.nihongo.srs.ui.exam

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nihongo.srs.NihongoApp
import com.nihongo.srs.data.UserProgress
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.data.VocabRepository
import com.nihongo.srs.speech.JapaneseSpeaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** TODAY = words learned or reviewed today. ALL = every learned word. */
enum class ExamScope { TODAY, ALL }

data class ExamUiState(
    val scope: ExamScope = ExamScope.TODAY,
    val isLoading: Boolean = true,
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    /** Option the user tapped for the current question, or null. */
    val selected: Int? = null,
    /** Number of original (non-retry) questions. */
    val totalOriginal: Int = 0,
    val firstTryCorrect: Int = 0,
    val wrongWords: List<VocabEntity> = emptyList(),
) {
    val current: Question? get() = questions.getOrNull(index)
    val isFinished: Boolean get() = !isLoading && questions.isNotEmpty() && index >= questions.size
    val isEmpty: Boolean get() = !isLoading && questions.isEmpty()
    val scorePercent: Int get() = if (totalOriginal == 0) 0 else firstTryCorrect * 100 / totalOriginal
}

/**
 * Multiple-choice exam on learned words (minimum 20 questions).
 * A wrong answer is asked again later in the same exam until you get it right,
 * and that word goes back to Stage 0 so it is reviewed again tomorrow.
 */
class ExamViewModel(
    private val repo: VocabRepository,
    private val speaker: JapaneseSpeaker,
    scope: ExamScope,
) : ViewModel() {

    private val generator = ExamGenerator()
    private var distractors: List<VocabEntity> = emptyList()
    private var progressById: Map<Long, UserProgress> = emptyMap()

    private val _state = MutableStateFlow(ExamUiState(scope = scope))
    val state: StateFlow<ExamUiState> = _state.asStateFlow()

    val ttsStatus = speaker.status

    init {
        viewModelScope.launch {
            val learned = repo.getLearned()
            val pool = if (scope == ExamScope.ALL) learned else learned.filter(repo::isFromToday)
            distractors = repo.getAllWords()
            progressById = learned.mapNotNull { it.progress }.associateBy { it.vocabId }
            val questions = generator.build(pool.map { it.vocab }, distractors)
            _state.update {
                it.copy(isLoading = false, questions = questions, totalOriginal = questions.size)
            }
            playIfListening()
        }
    }

    fun select(option: Int) {
        val s = _state.value
        val q = s.current ?: return
        if (s.selected != null) return
        val correct = option == q.correctIndex
        _state.update { st ->
            st.copy(
                selected = option,
                firstTryCorrect = st.firstTryCorrect + if (correct && !q.isRetry) 1 else 0,
                wrongWords = if (correct || st.wrongWords.any { it.id == q.word.id }) st.wrongWords
                else st.wrongWords + q.word,
                // Wrong -> ask this word again at the end of the exam.
                questions = if (correct) st.questions else st.questions + generator.retry(q, distractors),
            )
        }
        speaker.speak(q.word.reading)
    }

    fun next() {
        _state.update { it.copy(index = it.index + 1, selected = null) }
        if (_state.value.isFinished) onFinished() else playIfListening()
    }

    fun replay() {
        _state.value.current?.let { speaker.speak(it.word.reading) }
    }

    private fun playIfListening() {
        val q = _state.value.current ?: return
        if (q.type == QuestionType.LISTEN) speaker.speak(q.word.reading)
    }

    /** Words missed in the exam go back to Stage 0 (review tomorrow). */
    private fun onFinished() = viewModelScope.launch {
        _state.value.wrongWords.forEach { w ->
            progressById[w.id]?.let { repo.submitReview(it, correct = false) }
        }
    }

    companion object {
        const val ARG_SCOPE = "scope"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NihongoApp
                val handle: SavedStateHandle = createSavedStateHandle()
                val scope = handle.get<String>(ARG_SCOPE)
                    ?.let { runCatching { ExamScope.valueOf(it) }.getOrNull() } ?: ExamScope.TODAY
                ExamViewModel(app.repository, app.speaker, scope)
            }
        }
    }
}
