package com.nihongo.srs.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nihongo.srs.NihongoApp
import com.nihongo.srs.data.VocabRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val dueToday: Int = 0,
    val newWords: Int = 0,
    val learning: Int = 0,
    val mastered: Int = 0,
)

class HomeViewModel(private val repo: VocabRepository) : ViewModel() {

    /** Changes after midnight so "due today" counts stay correct. */
    private val endOfToday = MutableStateFlow(repo.endOfToday())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        endOfToday.flatMapLatest { repo.observeDueCount(it) },
        repo.observeNewWordCount(),
        repo.observeLearningCount(),
        repo.observeMasteredCount(),
    ) { due, new, learning, mastered ->
        HomeUiState(due, new, learning, mastered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // First launch: load the bundled N5 words + speaking phrases.
        viewModelScope.launch { repo.seedIfEmpty() }
    }

    /** Call when the Home screen becomes visible again. */
    fun refreshDay() {
        endOfToday.value = repo.endOfToday()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel((this[APPLICATION_KEY] as NihongoApp).repository) }
        }
    }
}
