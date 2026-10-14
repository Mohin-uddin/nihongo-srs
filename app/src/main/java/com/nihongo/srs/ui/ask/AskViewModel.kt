package com.nihongo.srs.ui.ask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nihongo.srs.NihongoApp
import com.nihongo.srs.ai.AskResult
import com.nihongo.srs.ai.AskWord
import com.nihongo.srs.ai.GeminiClient
import com.nihongo.srs.data.AppPrefs
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.data.VocabRepository
import com.nihongo.srs.speech.JapaneseSpeaker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AskState {
    data object Idle : AskState
    data object Loading : AskState
    data class Answer(val question: String, val result: AskResult) : AskState
    data class Error(val message: String) : AskState
}

/**
 * "Ask in Banglish": type anything in Banglish, Bangla or English and get it in Japanese.
 * Words already in the app are found offline; the translation needs the user's Gemini key.
 */
class AskViewModel(
    private val repo: VocabRepository,
    private val prefs: AppPrefs,
    private val speaker: JapaneseSpeaker,
) : ViewModel() {

    val input = MutableStateFlow("")
    private val submitted = MutableStateFlow("")

    private val _state = MutableStateFlow<AskState>(AskState.Idle)
    val state: StateFlow<AskState> = _state

    private val _hasKey = MutableStateFlow(prefs.geminiKey.isNotBlank())
    val hasKey: StateFlow<Boolean> = _hasKey

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    /** Words and phrases in the app whose meaning matches what was typed (works offline). */
    val localMatches: StateFlow<List<VocabEntity>> =
        combine(repo.observeAll(VocabEntity.TYPE_WORD), repo.observePhrases(), submitted) { words, phrases, q ->
            val query = q.trim()
            if (query.length < 2) emptyList()
            else (words.map { it.vocab } + phrases).filter { it.matches(query) }.take(8)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveKey(key: String) {
        prefs.geminiKey = key
        _hasKey.value = key.isNotBlank()
        if (key.isNotBlank()) _messages.tryEmit("Key saved on this phone • key সেভ হয়েছে")
    }

    fun ask() {
        val q = input.value.trim()
        if (q.isEmpty()) return
        submitted.value = q
        val key = prefs.geminiKey
        if (key.isBlank()) {
            _state.value = AskState.Idle
            return
        }
        _state.value = AskState.Loading
        viewModelScope.launch {
            _state.value = try {
                AskState.Answer(q, GeminiClient.ask(key, q))
            } catch (e: GeminiClient.AskException) {
                AskState.Error(e.message ?: "Error")
            } catch (e: Exception) {
                AskState.Error("Something went wrong, try again • সমস্যা হয়েছে, আবার চেষ্টা করুন")
            }
        }
    }

    fun speak(kana: String, slow: Boolean = false) = speaker.speak(kana.replace(" ", ""), slow)

    /** Saves a word to "My own words", with the translated sentence as its example. */
    fun saveWord(w: AskWord, sentence: AskResult) = viewModelScope.launch {
        val added = repo.addVocab(
            VocabEntity(
                japanese = w.japanese.trim(),
                reading = w.kana.replace(" ", "").trim(),
                romaji = w.romaji.trim(),
                meaning = w.english.trim(),
                meaningBn = w.bangla.trim(),
                example = sentence.japanese,
                exampleReading = sentence.kana,
                exampleEn = sentence.english,
                exampleBn = sentence.bangla,
                level = VocabEntity.LEVEL_MINE,
            )
        )
        _messages.emit(
            if (added) "${w.japanese} added to My own words • যোগ হয়েছে"
            else "${w.japanese} is already in the app • শব্দটি আগেই আছে"
        )
    }

    /** Saves the whole sentence as a card in "My own words". */
    fun saveSentence(r: AskResult) = viewModelScope.launch {
        val added = repo.addVocab(
            VocabEntity(
                japanese = r.japanese,
                reading = r.kana.replace(" ", ""),
                romaji = r.romaji,
                meaning = r.english,
                meaningBn = r.bangla,
                level = VocabEntity.LEVEL_MINE,
            )
        )
        _messages.emit(if (added) "Sentence saved • বাক্য সেভ হয়েছে" else "Already saved • আগেই সেভ করা আছে")
    }

    private fun VocabEntity.matches(q: String): Boolean =
        listOf(japanese, reading, romaji, meaning, meaningBn).any { it.contains(q, ignoreCase = true) } ||
            meaningBn.split("/", ",").any { part -> part.trim().length >= 2 && q.contains(part.trim(), ignoreCase = true) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NihongoApp
                AskViewModel(app.repository, app.prefs, app.speaker)
            }
        }
    }
}
