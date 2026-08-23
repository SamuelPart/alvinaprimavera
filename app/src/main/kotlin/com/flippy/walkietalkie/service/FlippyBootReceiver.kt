package com.flippy.walkietalkie.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Tras reiniciar el teléfono, vuelve a encender el servicio de
 * escucha de Flippy para que los audios sigan llegando 🔊
 */
class FlippyBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.i("FlippyBootReceiver", "Teléfono reiniciado → Flippy vuelve a la escucha 📡")
        runCatching {
            WalkieTalkieForegroundService.startService(context)
        }.onFailure {
            Log.w("FlippyBootReceiver", "No se pudo arrancar tras el reinicio", it)
        }
    }
}
