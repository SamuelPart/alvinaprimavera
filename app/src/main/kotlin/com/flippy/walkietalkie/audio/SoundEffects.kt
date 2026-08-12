package com.flippy.walkietalkie.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

object SoundEffects {

    private const val SAMPLE_RATE = 44100
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Plays Motorola Classic Dual-Tone Roger Beep + trailing squelch noise
     */
    fun playRogerBeep() {
        scope.launch {
            try {
                val duration1 = 0.07 // 70ms
                val duration2 = 0.08 // 80ms
                val squelchDuration = 0.08 // 80ms

                val numSamples1 = (duration1 * SAMPLE_RATE).toInt()
                val numSamples2 = (duration2 * SAMPLE_RATE).toInt()
                val numSamplesSquelch = (squelchDuration * SAMPLE_RATE).toInt()
                val totalSamples = numSamples1 + numSamples2 + numSamplesSquelch

                val buffer = ShortArray(totalSamples)

                // Tone 1: 1150 Hz
                for (i in 0 until numSamples1) {
                    val angle = 2.0 * Math.PI * i / (SAMPLE_RATE / 1150.0)
                    buffer[i] = (sin(angle) * Short.MAX_VALUE * 0.35).toInt().toShort()
                }

                // Tone 2: 880 Hz
                for (i in 0 until numSamples2) {
                    val angle = 2.0 * Math.PI * i / (SAMPLE_RATE / 880.0)
                    val idx = numSamples1 + i
                    buffer[idx] = (sin(angle) * Short.MAX_VALUE * 0.35).toInt().toShort()
                }

                // Trailing Squelch Noise
                for (i in 0 until numSamplesSquelch) {
                    val idx = numSamples1 + numSamples2 + i
                    val noise = (Random.nextDouble() * 2.0 - 1.0)
                    val envelope = 1.0 - (i.toDouble() / numSamplesSquelch)
                    buffer[idx] = (noise * Short.MAX_VALUE * 0.20 * envelope).toInt().toShort()
                }

                playPcmBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Mechanical PTT button click
     */
    fun playPttPressSound() {
        scope.launch {
            try {
                val samples = (0.04 * SAMPLE_RATE).toInt()
                val buffer = ShortArray(samples)
                for (i in 0 until samples) {
                    val freq = 450.0 - (i.toDouble() / samples) * 350.0
                    val angle = 2.0 * Math.PI * i / (SAMPLE_RATE / freq)
                    val env = 1.0 - (i.toDouble() / samples)
                    buffer[i] = (sin(angle) * Short.MAX_VALUE * 0.3 * env).toInt().toShort()
                }
                playPcmBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Mechanical PTT button release
     */
    fun playPttReleaseSound() {
        scope.launch {
            try {
                val samples = (0.03 * SAMPLE_RATE).toInt()
                val buffer = ShortArray(samples)
                for (i in 0 until samples) {
                    val freq = 320.0 - (i.toDouble() / samples) * 260.0
                    val angle = 2.0 * Math.PI * i / (SAMPLE_RATE / freq)
                    val env = 1.0 - (i.toDouble() / samples)
                    buffer[i] = (sin(angle) * Short.MAX_VALUE * 0.2 * env).toInt().toShort()
                }
                playPcmBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Radio tuning dial tick
     */
    fun playDialTick() {
        scope.launch {
            try {
                val samples = (0.025 * SAMPLE_RATE).toInt()
                val buffer = ShortArray(samples)
                for (i in 0 until samples) {
                    val angle = 2.0 * Math.PI * i / (SAMPLE_RATE / 900.0)
                    val env = 1.0 - (i.toDouble() / samples)
                    buffer[i] = (sin(angle) * Short.MAX_VALUE * 0.15 * env).toInt().toShort()
                }
                playPcmBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Incoming audio notification chirp
     */
    fun playIncomingAlert() {
        scope.launch {
            try {
                val samples = (0.12 * SAMPLE_RATE).toInt()
                val buffer = ShortArray(samples)
                for (i in 0 until samples) {
                    val freq = if (i < samples / 2) 1400.0 else 1800.0
                    val angle = 2.0 * Math.PI * i / (SAMPLE_RATE / freq)
                    buffer[i] = (sin(angle) * Short.MAX_VALUE * 0.25).toInt().toShort()
                }
                playPcmBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playPcmBuffer(buffer: ShortArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(Math.max(buffer.size * 2, minBufferSize))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        // Release after finish
        scope.launch {
            kotlinx.coroutines.delay((buffer.size * 1000L / SAMPLE_RATE) + 100)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {}
        }
    }
}
