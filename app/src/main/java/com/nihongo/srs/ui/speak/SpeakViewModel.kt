package com.nihongo.srs.ui.speak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nihongo.srs.NihongoApp
import com.nihongo.srs.data.ProgressRepository
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.data.VocabRepository
import com.nihongo.srs.speech.JapaneseSpeaker
import com.nihongo.srs.speech.PronunciationChecker
import com.nihongo.srs.speech.PronunciationScorer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State of the microphone check for the current phrase. */
sealed interface MicState {
    data object Idle : MicState
    data object Listening : MicState
    data class Result(val heard: String, val score: Int) : MicState
    data class Error(val message: String) : MicState
}

data class SpeakUiState(
    val index: Int = 0,
    val showMeaning: Boolean = true,
    val mic: MicState = MicState.Idle,
)

/**
 * Speaking practice with the "shadowing" method:
 * 1) listen (normal / slow)  2) repeat out loud  3) let the phone check you.
 */
class SpeakViewModel(
    repo: VocabRepository,
    private val speaker: JapaneseSpeaker,
    private val checker: PronunciationChecker,
    private val progress: ProgressRepository,
) : ViewModel() {

    private val allPhrases: StateFlow<List<VocabEntity>> = repo.observePhrases()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Topic filter; null = all topics. */
    val category = MutableStateFlow<String?>(null)

    val categories: StateFlow<List<String>> = allPhrases
        .map { list -> list.map { it.category.ifBlank { OTHER } }.distinct() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val phrases: StateFlow<List<VocabEntity>> = combine(allPhrases, category) { list, c ->
        if (c == null) list else list.filter { it.category.ifBlank { OTHER } == c }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _state = MutableStateFlow(SpeakUiState())
    val state: StateFlow<SpeakUiState> = _state.asStateFlow()

    val ttsStatus = speaker.status
    val micAvailable: Boolean get() = checker.isAvailable

    private fun current(): VocabEntity? = phrases.value.getOrNull(_state.value.index)

    fun listen(slow: Boolean) {
        current()?.let { speaker.speak(it.reading.replace(" ", ""), slow) }
    }

    fun next() = move(+1)
    fun previous() = move(-1)

    private fun move(delta: Int) {
        val size = phrases.value.size
        if (size == 0) return
        checker.stop()
        _state.update {
            it.copy(index = (it.index + delta + size) % size, mic = MicState.Idle)
        }
    }

    fun selectCategory(c: String?) {
        checker.stop()
        category.value = c
        _state.update { it.copy(index = 0, mic = MicState.Idle) }
    }

    fun toggleMeaning() = _state.update { it.copy(showMeaning = !it.showMeaning) }

    /** Start the microphone. Caller must already hold RECORD_AUDIO permission. */
    fun startSpeaking() {
        val target = current() ?: return
        speaker.stop()
        checker.start(object : PronunciationChecker.Callback {
            override fun onListening() = _state.update { it.copy(mic = MicState.Listening) }

            override fun onResult(candidates: List<String>) {
                val score = PronunciationScorer.score(candidates, target.japanese, target.reading)
                viewModelScope.launch { progress.log(spoken = 1) }
                _state.update {
                    it.copy(mic = MicState.Result(candidates.firstOrNull().orEmpty(), score))
                }
            }

            override fun onError(message: String) =
                _state.update { it.copy(mic = MicState.Error(message)) }
        })
    }

    fun onMicPermissionDenied() = _state.update {
        it.copy(mic = MicState.Error("Microphone permission needed • মাইক্রোফোনের অনুমতি দিন"))
    }

    override fun onCleared() {
        checker.destroy()
        speaker.stop()
    }

    companion object {
        const val OTHER = "Other • অন্যান্য"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NihongoApp
                SpeakViewModel(app.repository, app.speaker, PronunciationChecker(app), app.progress)
            }
        }
    }
}
