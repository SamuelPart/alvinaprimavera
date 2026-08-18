package com.flippy.walkietalkie.audio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.io.File
import java.util.Locale

// ============================================================
//  🎙️➡️📝 Transcriptor de voz a texto
//  Usa el reconocedor de voz DEL PROPIO TELÉFONO (gratis, sin
//  nubes externas). En Android 13+ le pasamos el audio grabado
//  directamente (EXTRA_AUDIO_SOURCE) y nos devuelve el texto.
//  Si el dispositivo no lo soporta, devuelve null y la app
//  solo muestra «🎙️ Audio de voz» (nada de textos inventados).
// ============================================================

object SpeechTranscriber {

    private const val TAG = "FlippySTT"
    private const val WATCHDOG_MS = 12_000L

    /** Transcribe PCM crudo (16-bit, mono) de forma asíncrona. */
    fun transcribeAsync(
        context: Context,
        pcmBytes: ByteArray,
        sampleRate: Int,
        onResult: (String?) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < 33 ||
            !SpeechRecognizer.isRecognitionAvailable(context)
        ) {
            Log.i(TAG, "Este dispositivo no soporta transcripción de archivos")
            onResult(null)
            return
        }
        Handler(Looper.getMainLooper()).post {
            runCatching {
                runRecognition(context, pcmBytes, sampleRate, onResult)
            }.onFailure {
                Log.e(TAG, "No se pudo iniciar el reconocedor", it)
                onResult(null)
            }
        }
    }

    private fun runRecognition(
        context: Context,
        pcmBytes: ByteArray,
        sampleRate: Int,
        onResult: (String?) -> Unit
    ) {
        val pcmFile = File.createTempFile("flippy_stt_", ".pcm", context.cacheDir)
        pcmFile.writeBytes(pcmBytes)

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val handler = Handler(Looper.getMainLooper())
        var bestSoFar: String? = null
        var delivered = false

        fun pickBest(bundle: Bundle?): String? =
            bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }

        fun done(text: String?) {
            if (delivered) return
            delivered = true
            runCatching { recognizer.destroy() }
            runCatching { pcmFile.delete() }
            Log.i(TAG, "Transcripción: ${text ?: "(sin texto)"}")
            onResult(text)
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {
                pickBest(partialResults)?.let { bestSoFar = it }
            }
            override fun onResults(results: Bundle?) {
                done(pickBest(results) ?: bestSoFar)
            }
            override fun onError(error: Int) {
                Log.w(TAG, "Reconocedor error $error")
                done(bestSoFar)
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // El audio que grabó el usuario va DIRECTO al reconocedor
                putExtra(
                    RecognizerIntent.EXTRA_AUDIO_SOURCE,
                    ParcelFileDescriptor.open(
                        pcmFile,
                        ParcelFileDescriptor.MODE_READ_ONLY
                    )
                )
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLE_RATE, sampleRate)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, 2) // PCM 16-bit
            }
        }

        recognizer.setRecognitionListener(listener)
        // Vigilante: si en 12s no responde, cerramos con el mejor texto parcial
        handler.postDelayed({ done(bestSoFar) }, WATCHDOG_MS)
        runCatching { recognizer.startListening(intent) }
            .onFailure { done(null) }
    }
}
