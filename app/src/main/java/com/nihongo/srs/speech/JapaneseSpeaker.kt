package com.nihongo.srs.speech

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Thin wrapper around Android's built-in TextToSpeech for Japanese.
 *
 * Free and offline once the phone has the Japanese voice installed
 * (Settings > Text-to-speech > Google engine > Install voice data > Japanese).
 */
class JapaneseSpeaker(context: Context) : TextToSpeech.OnInitListener {

    enum class Status { LOADING, READY, MISSING_VOICE, UNAVAILABLE }

    private val _status = MutableStateFlow(Status.LOADING)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val tts = TextToSpeech(context.applicationContext, this)

    override fun onInit(result: Int) {
        if (result != TextToSpeech.SUCCESS) {
            _status.value = Status.UNAVAILABLE
            return
        }
        val lang = tts.setLanguage(Locale.JAPAN)
        _status.value = when (lang) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> Status.MISSING_VOICE
            else -> Status.READY
        }
    }

    /**
     * Speaks [text]. Use [slow] = true for shadowing practice (0.6x speed).
     * Pass the kana reading when the kanji could be read in more than one way.
     */
    fun speak(text: String, slow: Boolean = false) {
        if (_status.value != Status.READY) return
        tts.setSpeechRate(if (slow) 0.6f else 0.9f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun stop() = tts.stop()

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    companion object {
        /** Opens the system screen to download the Japanese TTS voice (free). */
        fun installVoiceIntent(): Intent =
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
