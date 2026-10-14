package com.nihongo.srs

import android.app.Application
import com.nihongo.srs.data.AppDatabase
import com.nihongo.srs.data.VocabRepository
import com.nihongo.srs.speech.JapaneseSpeaker
import com.nihongo.srs.srs.SpacedRepetitionEngine

/**
 * Application class doing simple manual dependency injection.
 * (No Hilt/Dagger needed for an app this size.)
 */
class NihongoApp : Application() {

    val engine by lazy { SpacedRepetitionEngine() }
    val database by lazy { AppDatabase.get(this) }
    val repository by lazy { VocabRepository(database.vocabDao(), engine) }

    /** One shared TTS engine for the whole app. */
    val speaker by lazy { JapaneseSpeaker(this) }
}
