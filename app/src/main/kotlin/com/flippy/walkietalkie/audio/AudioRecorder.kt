package com.flippy.walkietalkie.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import com.flippy.walkietalkie.data.model.VoiceFilter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class AudioRecorder {

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false

    private val _micLevel = MutableStateFlow(0f)
    val micLevel = _micLevel.asStateFlow()

    private val _waveformSamples = MutableStateFlow(FloatArray(32) { 0f })
    val waveformSamples = _waveformSamples.asStateFlow()

    private val recordedPcmList = mutableListOf<Short>()
    private var recordingStartTime = 0L

    @SuppressLint("MissingPermission")
    fun startRecording(filter: VoiceFilter = VoiceFilter.RADIO) {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = Math.max(minBufferSize, 2048)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            audioRecord?.startRecording()
            isRecording = true
            recordedPcmList.clear()
            recordingStartTime = System.currentTimeMillis()

            SoundEffects.playPttPressSound()

            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                val readBuffer = ShortArray(1024)
                while (isRecording && isActive) {
                    val readCount = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0
                    if (readCount > 0) {
                        for (i in 0 until readCount) {
                            recordedPcmList.add(readBuffer[i])
                        }

                        // Calculate RMS level
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            sum += readBuffer[i] * readBuffer[i]
                        }
                        val rms = sqrt(sum / readCount)
                        val normalized = (rms / Short.MAX_VALUE * 100).toFloat().coerceIn(0f, 100f)
                        _micLevel.value = normalized

                        // Update waveform
                        val waveSlice = FloatArray(32)
                        val step = readCount / 32
                        for (j in 0 until 32) {
                            val sample = readBuffer[Math.min(j * step, readCount - 1)]
                            waveSlice[j] = (sample.toFloat() / Short.MAX_VALUE).coerceIn(-1f, 1f)
                        }
                        _waveformSamples.value = waveSlice
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
        }
    }

    suspend fun stopRecording(filter: VoiceFilter = VoiceFilter.RADIO): AudioResult? = withContext(Dispatchers.IO) {
        if (!isRecording) return@withContext null

        isRecording = false
        recordingJob?.cancel()
        _micLevel.value = 0f
        _waveformSamples.value = FloatArray(32) { 0f }

        SoundEffects.playPttReleaseSound()

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val duration = (System.currentTimeMillis() - recordingStartTime) / 1000f
        if (duration < 0.3f || recordedPcmList.isEmpty()) {
            return@withContext null
        }

        // Apply DSP Voice Filter
        val rawPcm = recordedPcmList.toShortArray()
        val filteredPcm = VoiceFilterProcessor.applyFilter(rawPcm, filter, SAMPLE_RATE)

        // Convert to WAV bytes and Base64
        val wavBytes = pcmToWav(filteredPcm, SAMPLE_RATE)
        val base64 = "data:audio/wav;base64," + Base64.encodeToString(wavBytes, Base64.NO_WRAP)

        return@withContext AudioResult(
            pcmData = filteredPcm,
            wavBytes = wavBytes,
            base64Audio = base64,
            duration = duration
        )
    }

    private fun pcmToWav(pcmData: ShortArray, sampleRate: Int): ByteArray {
        val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in pcmData) {
            byteBuffer.putShort(s)
        }
        val pcmBytes = byteBuffer.array()

        val totalDataLen = pcmBytes.size + 36
        val totalAudioLen = pcmBytes.size
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM format
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * 2).toByte()
        header[33] = 0
        header[34] = 16 // 16-bit
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()

        val output = ByteArrayOutputStream()
        output.write(header)
        output.write(pcmBytes)
        return output.toByteArray()
    }
}

data class AudioResult(
    val pcmData: ShortArray,
    val wavBytes: ByteArray,
    val base64Audio: String,
    val duration: Float
)
