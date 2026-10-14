package com.nihongo.srs.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Listens to the user speaking Japanese with Android's built-in SpeechRecognizer
 * and reports what it heard. Free, no API key.
 *
 * Works offline if the phone has the Japanese offline speech pack
 * (Google app > Settings > Voice > Offline speech recognition > 日本語).
 * Must be created and used on the main thread.
 */
class PronunciationChecker(private val context: Context) {

    interface Callback {
        fun onListening()
        fun onResult(candidates: List<String>)
        fun onError(message: String)
    }

    private var recognizer: SpeechRecognizer? = null

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(callback: Callback) {
        if (!isAvailable) {
            callback.onError("Speech recognition is not available on this phone • এই ফোনে স্পিচ রিকগনিশন নেই")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = callback.onListening()
                override fun onResults(results: Bundle?) {
                    val list = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        .orEmpty()
                    callback.onResult(list)
                }
                override fun onError(error: Int) = callback.onError(errorText(error))
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        recognizer?.startListening(intent)
    }

    fun stop() {
        recognizer?.stopListening()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun errorText(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "Didn't catch that, try again • বোঝা যায়নি, আবার বলুন"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Microphone permission needed • মাইক্রোফোনের অনুমতি দিন"
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Needs the offline Japanese speech pack or internet • অফলাইন জাপানি স্পিচ প্যাক ইনস্টল করুন"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            "Japanese recognition not installed • জাপানি ভাষা ইনস্টল নেই"
        else -> "Recognizer error ($code), try again • আবার চেষ্টা করুন"
    }
}
