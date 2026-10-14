package com.nihongo.srs.ui.study

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** LEARNED = every word you have started, TODAY = learned or reviewed today, N5 / N4 / MINE = the whole list. */
enum class StudyFilter(val en: String, val bn: String) {
    LEARNED("Learned", "শেখা"),
    TODAY("Today", "আজ"),
    N5("All N5", "N5 সব"),
    N4("All N4", "N4 সব"),
    MINE("Mine", "আমার যোগ করা"),
}

/**
 * "My Words": every learned word with its answer and example sentence visible,
 * so you can read through them any time without flipping cards.
 * Browsing here does not change the review schedule.
 */
class StudyListViewModel(
    private val repo: VocabRepository,
    private val speaker: JapaneseSpeaker,
) : ViewModel() {

    val filter = MutableStateFlow(StudyFilter.LEARNED)
    val sentencesOnly = MutableStateFlow(false)

    /** Search box: Japanese, kana, romaji, English or Bangla, including example sentences. */
    val query = MutableStateFlow("")

    val items: StateFlow<List<VocabWithProgress>> =
        combine(repo.observeLearned(), repo.observeAll(VocabEntity.TYPE_WORD), filter, query) { learned, all, f, q ->
            val text = q.trim()
            when {
                // A search looks through every word in the app, learned or not.
                text.isNotEmpty() -> all.filter { it.vocab.matches(text) }
                f == StudyFilter.LEARNED -> learned
                f == StudyFilter.TODAY -> learned.filter(repo::isFromToday)
                f == StudyFilter.N5 -> all.filter { it.vocab.level == "N5" }
                f == StudyFilter.N4 -> all.filter { it.vocab.level == "N4" }
                else -> all.filter { it.vocab.level == VocabEntity.LEVEL_MINE }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Nothing learned yet: show the whole N5 list instead of an empty page.
        viewModelScope.launch { if (repo.getLearned().isEmpty()) filter.value = StudyFilter.N5 }
    }

    private fun VocabEntity.matches(q: String): Boolean =
        listOf(japanese, reading, romaji, meaning, meaningBn, example, exampleReading, exampleEn, exampleBn)
            .any { it.contains(q, ignoreCase = true) }

    fun speakWord(v: VocabEntity) = speaker.speak(v.reading)

    fun speakExample(v: VocabEntity, slow: Boolean = false) =
        speaker.speak(v.exampleReading.ifBlank { v.example }.replace(" ", ""), slow)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NihongoApp
                StudyListViewModel(app.repository, app.speaker)
            }
        }
    }
}
