package com.nihongo.srs.news

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RadioState(
    val episode: RadioEpisode? = null,
    val isPreparing: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val speed: Float = 1f,
    val error: String? = null,
)

/**
 * Streams NHK radio news with Android's built-in MediaPlayer (no extra library).
 * Kept by the Application so audio continues while you move between screens.
 */
class RadioPlayer(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    private var ticker: Job? = null

    private val _state = MutableStateFlow(RadioState())
    val state: StateFlow<RadioState> = _state.asStateFlow()

    fun play(episode: RadioEpisode) {
        if (_state.value.episode?.audioUrl == episode.audioUrl && player != null) {
            toggle()
            return
        }
        release()
        _state.value = RadioState(episode = episode, isPreparing = true, speed = _state.value.speed)
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setOnPreparedListener { mp ->
                applySpeed(mp, _state.value.speed)
                mp.start()
                _state.update { it.copy(isPreparing = false, isPlaying = true, durationMs = mp.duration) }
                startTicker()
            }
            setOnCompletionListener {
                _state.update { it.copy(isPlaying = false, positionMs = it.durationMs) }
            }
            setOnErrorListener { _, _, _ ->
                _state.update {
                    it.copy(isPreparing = false, isPlaying = false,
                        error = "Couldn't play. Check the internet. • চালানো যায়নি, ইন্টারনেট দেখুন")
                }
                true
            }
            try {
                setDataSource(episode.audioUrl)
                prepareAsync()
            } catch (e: Exception) {
                _state.update { it.copy(isPreparing = false, error = "Couldn't play • চালানো যায়নি") }
            }
        }
    }

    fun toggle() {
        val mp = player ?: return
        if (_state.value.isPreparing) return
        if (mp.isPlaying) {
            mp.pause()
            _state.update { it.copy(isPlaying = false) }
        } else {
            if (_state.value.positionMs >= _state.value.durationMs - 500) mp.seekTo(0)
            mp.start()
            _state.update { it.copy(isPlaying = true) }
            startTicker()
        }
    }

    fun seekTo(ms: Int) {
        val mp = player ?: return
        if (_state.value.isPreparing) return
        val target = ms.coerceIn(0, _state.value.durationMs)
        mp.seekTo(target)
        _state.update { it.copy(positionMs = target) }
    }

    fun skip(deltaMs: Int) = seekTo(_state.value.positionMs + deltaMs)

    /** 0.75x helps a beginner catch words; 1x is real speed. */
    fun setSpeed(speed: Float) {
        _state.update { it.copy(speed = speed) }
        val mp = player ?: return
        if (!_state.value.isPreparing) {
            val wasPlaying = mp.isPlaying
            applySpeed(mp, speed)
            // Changing speed starts playback on some phones; keep it paused if it was paused.
            if (!wasPlaying && mp.isPlaying) mp.pause()
        }
    }

    private fun applySpeed(mp: MediaPlayer, speed: Float) {
        runCatching { mp.playbackParams = mp.playbackParams.setSpeed(speed) }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val mp = player ?: break
                runCatching { _state.update { it.copy(positionMs = mp.currentPosition) } }
                if (!_state.value.isPlaying) break
                delay(500)
            }
        }
    }

    fun stop() {
        release()
        _state.value = RadioState(speed = _state.value.speed)
    }

    private fun release() {
        ticker?.cancel()
        player?.runCatching { stop() }
        player?.release()
        player = null
    }
}
