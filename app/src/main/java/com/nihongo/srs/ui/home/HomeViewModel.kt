package com.nihongo.srs.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nihongo.srs.NihongoApp
import com.nihongo.srs.data.AppPrefs
import com.nihongo.srs.data.LevelProgress
import com.nihongo.srs.data.ProgressRepository
import com.nihongo.srs.data.StudyDay
import com.nihongo.srs.data.VocabEntity
import com.nihongo.srs.data.VocabRepository
import com.nihongo.srs.reminder.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val dueToday: Int = 0,
    val newN5: Int = 0,
    val newN4: Int = 0,
    val newMine: Int = 0,
    val learning: Int = 0,
    val mastered: Int = 0,
    val streak: Int = 0,
    val today: StudyDay = StudyDay(""),
    val levels: List<LevelProgress> = emptyList(),
) {
    /** Today's 4 goals. */
    val goals: List<Goal>
        get() = listOf(
            Goal("Finish today's reviews", "আজকের রিভিউ শেষ করুন", null, dueToday == 0),
            Goal("Learn $GOAL_NEW_WORDS new words", "${GOAL_NEW_WORDS}টি নতুন শব্দ শিখুন",
                "${today.learned.coerceAtMost(GOAL_NEW_WORDS)}/$GOAL_NEW_WORDS", today.learned >= GOAL_NEW_WORDS),
            Goal("Take an exam", "একটি পরীক্ষা দিন", null, today.exams >= 1),
            Goal("Speak $GOAL_SPOKEN phrases", "মাইকে ${GOAL_SPOKEN}টি বাক্য বলুন",
                "${today.spoken.coerceAtMost(GOAL_SPOKEN)}/$GOAL_SPOKEN", today.spoken >= GOAL_SPOKEN),
        )

    fun level(name: String): LevelProgress? = levels.firstOrNull { it.level == name }

    companion object {
        const val GOAL_NEW_WORDS = 10
        const val GOAL_SPOKEN = 5
    }
}

data class Goal(val en: String, val bn: String, val count: String?, val done: Boolean)

data class ReminderUi(val enabled: Boolean, val hour: Int, val minute: Int) {
    val label: String
        get() {
            val h12 = if (hour % 12 == 0) 12 else hour % 12
            return "%d:%02d %s".format(h12, minute, if (hour < 12) "AM" else "PM")
        }
}

class HomeViewModel(
    private val app: Application,
    private val repo: VocabRepository,
    private val prefs: AppPrefs,
    progress: ProgressRepository,
) : ViewModel() {

    /** Players sharing the phone, for the scoreboard on Home. */
    val players = (app as NihongoApp).players.players
    val activeId = app.players.activeId

    /** Changes after midnight so "due today" counts stay correct. */
    private val endOfToday = MutableStateFlow(repo.endOfToday())

    private val newCounts = combine(
        repo.observeNewWordCount("N5"),
        repo.observeNewWordCount("N4"),
        repo.observeNewWordCount(VocabEntity.LEVEL_MINE),
    ) { n5, n4, mine -> Triple(n5, n4, mine) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val counts = combine(
        endOfToday.flatMapLatest { repo.observeDueCount(it) },
        newCounts,
        repo.observeLearningCount(),
        repo.observeMasteredCount(),
    ) { due, new, learning, mastered ->
        HomeUiState(due, new.first, new.second, new.third, learning, mastered)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        counts,
        progress.observeStreak(),
        endOfToday.flatMapLatest { progress.observeToday() },
        progress.observeLevelProgress(),
    ) { c, streak, today, levels -> c.copy(streak = streak, today = today, levels = levels) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _reminder = MutableStateFlow(
        ReminderUi(prefs.reminderEnabled, prefs.reminderHour, prefs.reminderMinute)
    )
    val reminder: StateFlow<ReminderUi> = _reminder.asStateFlow()

    /** True once: on first launch we ask for notification permission. */
    val shouldAskNotificationPermission: Boolean
        get() = !prefs.notificationPermissionAsked

    fun onNotificationPermissionAsked() {
        prefs.notificationPermissionAsked = true
    }

    init {
        // First launch / app update: load the bundled N5 + N4 words and phrases.
        viewModelScope.launch { repo.syncStarterContentIfNeeded() }
    }

    /** Call when the Home screen becomes visible again. */
    fun refreshDay() {
        endOfToday.value = repo.endOfToday()
    }

    fun setReminder(enabled: Boolean, hour: Int = prefs.reminderHour, minute: Int = prefs.reminderMinute) {
        prefs.reminderEnabled = enabled
        prefs.reminderHour = hour
        prefs.reminderMinute = minute
        _reminder.value = ReminderUi(enabled, hour, minute)
        ReminderScheduler.schedule(app, enabled, hour, minute, replace = true)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NihongoApp
                HomeViewModel(app, app.repository, app.prefs, app.progress)
            }
        }
    }
}
