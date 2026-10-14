package com.nihongo.srs.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nihongo.srs.NihongoApp
import com.nihongo.srs.data.ExamResult
import com.nihongo.srs.data.LevelProgress
import com.nihongo.srs.data.ProgressRepository
import com.nihongo.srs.data.StageCount
import com.nihongo.srs.data.Streak
import com.nihongo.srs.data.StudyDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class ProgressUiState(
    val levels: List<LevelProgress> = emptyList(),
    val stages: List<StageCount> = emptyList(),
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val studyDays: Int = 0,
    val totalReviews: Int = 0,
    val accuracy: Int = 0,
    /** Last 7 days, oldest first; days with no study are filled with zeros. */
    val week: List<StudyDay> = emptyList(),
    val exams: List<ExamResult> = emptyList(),
) {
    val wordsLearned: Int get() = levels.sumOf { it.learned }
    val wordsMastered: Int get() = levels.sumOf { it.mastered }
}

class ProgressViewModel(progress: ProgressRepository) : ViewModel() {

    val state: StateFlow<ProgressUiState> = combine(
        progress.observeLevelProgress(),
        progress.observeStageCounts(),
        progress.observeAllDays(),
        progress.observeRecentExams(10),
    ) { levels, stages, days, exams ->
        val active = progress.activeDates(days)
        val today = LocalDate.now()
        val byDate = days.associateBy { it.date }
        val reviews = days.sumOf { it.reviewed }
        val correct = days.sumOf { it.reviewedCorrect }
        ProgressUiState(
            levels = levels.sortedBy { LEVEL_ORDER.indexOf(it.level).let { i -> if (i < 0) 99 else i } },
            stages = stages,
            streak = Streak.current(active, today),
            bestStreak = Streak.best(active),
            studyDays = active.size,
            totalReviews = reviews,
            accuracy = if (reviews == 0) 0 else correct * 100 / reviews,
            week = (6 downTo 0).map { back ->
                val d = today.minusDays(back.toLong()).toString()
                byDate[d] ?: StudyDay(d)
            },
            exams = exams,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    companion object {
        private val LEVEL_ORDER = listOf("N5", "N4", "Mine")

        val Factory = viewModelFactory {
            initializer { ProgressViewModel((this[APPLICATION_KEY] as NihongoApp).progress) }
        }
    }
}
