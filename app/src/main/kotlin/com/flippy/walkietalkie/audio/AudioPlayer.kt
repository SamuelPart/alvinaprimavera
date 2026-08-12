package com.flippy.walkietalkie.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class AudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPlayingId = MutableStateFlow<String?>(null)
    val currentPlayingId = _currentPlayingId.asStateFlow()

    fun playVoiceMessage(
        messageId: String,
        base64Audio: String?,
        speed: Float = 1.0f,
        onComplete: (() -> Unit)? = null
    ) {
        stop()

        if (base64Audio.isNullOrEmpty()) {
            // Play Roger Beep fallback
            SoundEffects.playRogerBeep()
            onComplete?.invoke()
            return
        }

        try {
            val cleanBase64 = if (base64Audio.contains(",")) {
                base64Audio.substringAfter(",")
            } else {
                base64Audio
            }

            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val tempFile = File.createTempFile("flippy_audio_", ".wav", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(decodedBytes) }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    playbackParams = PlaybackParams().apply { this.speed = speed }
                }

                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPlayingId.value = null
                    SoundEffects.playRogerBeep()
                    tempFile.delete()
                    onComplete?.invoke()
                }

                setOnErrorListener { _, _, _ ->
                    _isPlaying.value = false
                    _currentPlayingId.value = null
                    tempFile.delete()
                    true
                }

                start()
                _isPlaying.value = true
                _currentPlayingId.value = messageId
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
            _currentPlayingId.value = null
            onComplete?.invoke()
        }
    }

    fun stop() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isPlaying.value = false
        _currentPlayingId.value = null
    }
}
