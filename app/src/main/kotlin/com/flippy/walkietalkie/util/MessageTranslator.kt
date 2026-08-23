package com.flippy.walkietalkie.util

import android.util.Log
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

/**
 * 🌐 Traducción 100% GRATIS y en tu propio teléfono (Google ML Kit,
 * todo sucede EN EL DISPOSITIVO, sin servidores de pago):
 *
 * 1) Detecta el idioma del texto.
 * 2) Si es distinto al idioma de tu teléfono, lo traduce.
 * 3) La primera vez por idioma se descarga un modelito (~30 MB);
 *    después funciona rápido y hasta sin internet para ese par.
 *
 * Nunca rompe nada: si no se puede, simplemente no traduce.
 */
object MessageTranslator {

    private const val TAG = "Traductor"

    /** Idioma al que traducimos: el del teléfono (es, en, pt…). */
    val targetLanguage: String
        get() = Locale.getDefault().language.ifBlank { "es" }

    /**
     * Traduce SOLO si [text] está en OTRO idioma distinto al tuyo.
     * Llama a [onDone] con la traducción, o con null si no hacía falta
     * (mismo idioma) o no fue posible todavía.
     */
    fun translateIfForeign(text: String, onDone: (String?) -> Unit) {
        if (text.isBlank()) {
            onDone(null)
            return
        }
        try {
            LanguageIdentification.getClient().identifyLanguage(text)
                .addOnSuccessListener { detected ->
                    val lang = detected ?: "und"
                    if (lang == "und" || lang.equals(targetLanguage, ignoreCase = true)) {
                        onDone(null) // mismo idioma: no hace falta traducir ✌️
                        return@addOnSuccessListener
                    }
                    val sourceTag = TranslateLanguage.fromLanguageTag(lang)
                    val targetTag = TranslateLanguage.fromLanguageTag(targetLanguage)
                    if (sourceTag == null || targetTag == null) {
                        onDone(null)
                        return@addOnSuccessListener
                    }
                    val options = TranslatorOptions.Builder()
                        .setSourceLanguage(sourceTag)
                        .setTargetLanguage(targetTag)
                        .build()
                    val translator = Translation.getClient(options)
                    translator.downloadModelIfNeeded()
                        .addOnSuccessListener {
                            translator.translate(text)
                                .addOnSuccessListener { result -> onDone(result) }
                                .addOnFailureListener { onDone(null) }
                        }
                        .addOnFailureListener {
                            Log.w(TAG, "Modelo $lang → $targetLanguage aún no disponible")
                            onDone(null)
                        }
                }
                .addOnFailureListener { onDone(null) }
        } catch (e: Exception) {
            Log.e(TAG, "Traducción omitida", e)
            onDone(null)
        }
    }
}
