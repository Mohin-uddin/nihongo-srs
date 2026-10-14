package com.nihongo.srs.ui.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nihongo.srs.NihongoApp
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.data.VocabRepository
import com.nihongo.srs.data.VocabWithProgress
import com.nihongo.srs.speech.JapaneseSpeaker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Values typed into the "Add word" form. */
data class VocabForm(
    val japanese: String = "",
    val reading: String = "",
    val romaji: String = "",
    val meaning: String = "",
    val meaningBn: String = "",
) {
    val isValid: Boolean
        get() = japanese.isNotBlank() && reading.isNotBlank() &&
            (meaning.isNotBlank() || meaningBn.isNotBlank())
}

class ManageViewModel(
    private val repo: VocabRepository,
    private val speaker: JapaneseSpeaker,
) : ViewModel() {

    val form = MutableStateFlow(VocabForm())
    val query = MutableStateFlow("")

    /** One-off messages for a Snackbar. */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val words: StateFlow<List<VocabWithProgress>> =
        combine(repo.observeAll(VocabEntity.TYPE_WORD), query) { list, q ->
            if (q.isBlank()) list else list.filter {
                val v = it.vocab
                listOf(v.japanese, v.reading, v.romaji, v.meaning, v.meaningBn)
                    .any { f -> f.contains(q.trim(), ignoreCase = true) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateForm(transform: (VocabForm) -> VocabForm) {
        form.value = transform(form.value)
    }

    fun save() {
        val f = form.value
        if (!f.isValid) {
            _messages.tryEmit("Japanese, reading and one meaning are required • জাপানি, উচ্চারণ ও একটি অর্থ লিখুন")
            return
        }
        viewModelScope.launch {
            val added = repo.addVocab(
                VocabEntity(
                    japanese = f.japanese.trim(),
                    reading = f.reading.trim(),
                    romaji = f.romaji.trim(),
                    meaning = f.meaning.trim(),
                    meaningBn = f.meaningBn.trim(),
                    level = "N5",
                )
            )
            if (added) {
                form.value = VocabForm()
                _messages.emit("Word added • শব্দ যোগ হয়েছে")
            } else {
                _messages.emit("This word already exists • শব্দটি আগেই আছে")
            }
        }
    }

    fun preloadN5() = viewModelScope.launch {
        val n = repo.preloadStarterData()
        _messages.emit(
            if (n == 0) "N5 list already loaded • N5 তালিকা আগেই লোড করা আছে"
            else "$n items added • $n টি যোগ হয়েছে"
        )
    }

    fun delete(vocab: VocabEntity) = viewModelScope.launch { repo.deleteVocab(vocab) }

    fun resetProgress(vocab: VocabEntity) = viewModelScope.launch {
        repo.resetProgress(vocab.id)
        _messages.emit("Progress reset • অগ্রগতি রিসেট হয়েছে")
    }

    fun speak(vocab: VocabEntity) = speaker.speak(vocab.reading)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NihongoApp
                ManageViewModel(app.repository, app.speaker)
            }
        }
    }
}
