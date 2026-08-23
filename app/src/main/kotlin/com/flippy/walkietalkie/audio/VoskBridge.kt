package com.flippy.walkietalkie.audio

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

// ============================================================
//  🗣️➡️📝 VoskBridge — transcripción 100% GRATIS y OFFLINE
//  Funciona en cualquier Android (no depende de Google).
//  El modelo de español (~42 MB) se descarga UNA sola vez del
//  servidor gratuito de Vosk y queda en el teléfono para siempre.
//  Mientras se descarga, las primeras transcripciones esperan
//  su turno y se completan cuando el modelo esté listo ✨
// ============================================================

object VoskBridge {

    private const val TAG = "FlippyVosk"
    private const val MODEL_URL =
        "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip"
    private const val MODEL_FOLDER = "vosk-model-small-es-0.42"
    private const val ZIP_NAME = "vosk-es.zip"

    // Un solo hilo: descarga → carga → transcripciones (en orden)
    private val worker = Executors.newSingleThreadExecutor()
    private val preparing = AtomicBoolean(false)

    @Volatile
    private var model: Model? = null

    private fun modelDir(context: Context) = File(context.filesDir, MODEL_FOLDER)

    /** Se llama al abrir la app: empieza a preparar el modelo en segundo plano. */
    fun ensureModelConfigured(context: Context) {
        val app = context.applicationContext
        worker.execute { prepareModelIfNeeded(app) }
    }

    /** Transcribe PCM crudo (16-bit mono) de forma asíncrona.
     *  Si el modelo aún no está, lo prepara primero y luego transcribe. */
    fun transcribePcmAsync(
        context: Context,
        pcmBytes: ByteArray,
        sampleRate: Int,
        onResult: (String?) -> Unit
    ) {
        val app = context.applicationContext
        worker.execute {
            try {
                val text = transcribePcm(app, pcmBytes, sampleRate)
                android.os.Handler(android.os.Looper.getMainLooper())
                    .post { onResult(text) }
            } catch (e: Exception) {
                Log.e(TAG, "Error transcribiendo con Vosk", e)
                android.os.Handler(android.os.Looper.getMainLooper())
                    .post { onResult(null) }
            }
        }
    }

    // ---------------- Internos (siempre dentro del worker) ----------------

    @Synchronized
    private fun transcribePcm(context: Context, pcm: ByteArray, sampleRate: Int): String? {
        if (pcm.isEmpty()) return null
        if (!prepareModelIfNeeded(context)) return null
        val m = model ?: return null
        val recognizer = Recognizer(m, sampleRate.toFloat())
        return try {
            recognizer.acceptWaveForm(pcm, pcm.size)
            val text = JSONObject(recognizer.finalResult).optString("text").trim()
            if (text.isBlank()) null
            else text.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            } + "." // toque bonito: termina con punto ✨
        } catch (e: Exception) {
            Log.e(TAG, "Vosk no pudo reconocer el audio", e)
            null
        } finally {
            runCatching { recognizer.close() }
        }
    }

    /** Devuelve true si el modelo está listo para usar (lo descarga si falta). */
    @Synchronized
    private fun prepareModelIfNeeded(context: Context): Boolean {
        if (model != null) return true

        val dir = modelDir(context)
        val okFolder = File(dir, "am/final.mdl").exists()

        if (!okFolder) {
            if (preparing.get()) return false // otra ronda en curso
            preparing.set(true)
            try {
                Log.i(TAG, "Descargando modelo de español (~42 MB, una sola vez)…")
                val zip = File(context.cacheDir, ZIP_NAME)
                downloadModel(zip)
                unzipModel(zip, context.filesDir)
                zip.delete()
                Log.i(TAG, "Modelo descargado y descomprimido ✅")
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo descargar el modelo (se reintenta al siguiente uso)", e)
                runCatching { dir.deleteRecursively() }
                return false
            } finally {
                preparing.set(false)
            }
        }

        return try {
            model = Model(dir.absolutePath)
            Log.i(TAG, "Modelo Vosk cargado en memoria ✅")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Modelo dañado, se borrará para re-descargarlo", e)
            runCatching { dir.deleteRecursively() }
            false
        }
    }

    private fun downloadModel(dest: File) {
        val conn = URL(MODEL_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        try {
            conn.inputStream.use { input ->
                FileOutputStream(dest).use { out ->
                    val buf = ByteArray(64 * 1024)
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) {
                        out.write(buf, 0, n)
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun unzipModel(zipFile: File, destDir: File) {
        zipFile.inputStream().buffered().use { raw ->
            ZipInputStream(raw).use { zis ->
                var entry: ZipEntry?
                while (zis.nextEntry.also { entry = it } != null) {
                    val e = entry!!
                    // Seguridad: no salir de filesDir
                    val outFile = File(destDir, e.name)
                    if (!outFile.canonicalPath.startsWith(destDir.canonicalPath)) continue
                    if (e.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().buffered().use { out ->
                            zis.copyTo(out, 64 * 1024)
                        }
                    }
                    zis.closeEntry()
                }
            }
        }
    }
}
