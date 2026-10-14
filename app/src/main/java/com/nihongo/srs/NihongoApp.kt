package com.nihongo.srs

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.nihongo.srs.data.AppDatabase
import com.nihongo.srs.data.AppPrefs
import com.nihongo.srs.data.PlayerStore
import com.nihongo.srs.news.NewsRepository
import com.nihongo.srs.news.RadioPlayer
import com.nihongo.srs.data.ProgressRepository
import com.nihongo.srs.data.VocabRepository
import com.nihongo.srs.reminder.ReminderScheduler
import com.nihongo.srs.reminder.ReminderWorker
import com.nihongo.srs.speech.JapaneseSpeaker
import com.nihongo.srs.srs.SpacedRepetitionEngine

/**
 * Application class doing simple manual dependency injection.
 * (No Hilt/Dagger needed for an app this size.)
 */
class NihongoApp : Application() {

    val prefs by lazy { AppPrefs(this) }
    val engine by lazy { SpacedRepetitionEngine() }
    val database by lazy { AppDatabase.get(this) }
    val progress by lazy { ProgressRepository(database) }
    val repository by lazy { VocabRepository(database, engine, assets, prefs, progress) }

    /** Players sharing this phone and their points. */
    val players by lazy { PlayerStore(this) }

    /** NHK news headlines, radio episodes and prepared articles. */
    val news by lazy { NewsRepository(this, prefs, repository) }

    /** NHK radio news player; lives here so it keeps playing between screens. */
    val radio by lazy { RadioPlayer(this) }

    /** One shared TTS engine for the whole app. */
    val speaker by lazy { JapaneseSpeaker(this) }

    /** Work that should finish even if no screen is open (e.g. loading the word lists). */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Load / update the bundled N5 + N4 words as soon as the app starts (also after an update).
        appScope.launch { runCatching { repository.syncStarterContentIfNeeded() } }
        ReminderWorker.createChannel(this)
        ReminderScheduler.schedule(
            this, prefs.reminderEnabled, prefs.reminderHour, prefs.reminderMinute, replace = false
        )
    }
}
