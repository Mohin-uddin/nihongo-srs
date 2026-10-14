package com.nihongo.srs

import android.app.Application
import com.nihongo.srs.data.AppDatabase
import com.nihongo.srs.data.AppPrefs
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

    /** One shared TTS engine for the whole app. */
    val speaker by lazy { JapaneseSpeaker(this) }

    override fun onCreate() {
        super.onCreate()
        ReminderWorker.createChannel(this)
        ReminderScheduler.schedule(
            this, prefs.reminderEnabled, prefs.reminderHour, prefs.reminderMinute, replace = false
        )
    }
}
