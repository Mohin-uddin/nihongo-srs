package com.nihongo.srs.ui.review

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

/** REVIEW = today's due cards. LEARN = new words (Day 0). */
enum class SessionMode { REVIEW, LEARN }

data class CardItem(
    val vocab: VocabEntity,
    val progress: UserProgress?,
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
) {
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
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewUiState(mode = mode))
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    val ttsStatus = speaker.status

    init {
        load()
    }

    private fun load() = viewModelScope.launch {
        val cards = when (_state.value.mode) {
            SessionMode.REVIEW -> repo.getDueReviews().map { CardItem(it.vocab, it.progress) }
            SessionMode.LEARN -> repo.getNewWords(NEW_WORDS_PER_SESSION).map { CardItem(it, null) }
        }
        _state.update { it.copy(isLoading = false, cards = cards, index = 0, isFlipped = false) }
    }

    /** Tap on the card: show / hide the reading and meaning. */
    fun flip() {
        _state.update { it.copy(isFlipped = !it.isFlipped) }
        // Auto-play pronunciation when the answer is revealed.
        if (_state.value.isFlipped) speakCurrent()
    }

    fun speakCurrent(slow: Boolean = false) {
        val v = _state.value.current?.vocab ?: return
        // Speak the kana reading so the TTS never guesses the wrong kanji reading.
        speaker.speak(v.reading, slow)
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
            _state.update {
                it.copy(
                    index = it.index + 1,
                    isFlipped = false,
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
            _state.update {
                it.copy(index = it.index + 1, isFlipped = false, isBusy = false,
                    correctCount = it.correctCount + 1)
            }
        }
    }

    /** LEARN mode: skip this word for now (it stays in the new-word pool). */
    fun skip() {
        _state.update { it.copy(index = it.index + 1, isFlipped = false) }
    }

    override fun onCleared() {
        speaker.stop()
    }

    companion object {
        const val NEW_WORDS_PER_SESSION = 10
        const val ARG_MODE = "mode"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NihongoApp
                val handle: SavedStateHandle = createSavedStateHandle()
                val mode = handle.get<String>(ARG_MODE)
                    ?.let { runCatching { SessionMode.valueOf(it) }.getOrNull() }
                    ?: SessionMode.REVIEW
                ReviewViewModel(app.repository, app.speaker, mode)
            }
        }
    }
}
