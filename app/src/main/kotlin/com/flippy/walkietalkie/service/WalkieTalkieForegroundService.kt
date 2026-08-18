package com.flippy.walkietalkie.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.flippy.walkietalkie.FlippyApp
import com.flippy.walkietalkie.MainActivity
import com.flippy.walkietalkie.R
import com.flippy.walkietalkie.audio.SoundEffects
import com.flippy.walkietalkie.data.supabase.SupabaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================
//  📡 Servicio SIEMPRE VIVO: aunque cierres la app, juegues o
//  mires videos, aquí seguimos escuchando los audios que te
//  envían y los REPRODUCIMOS (si tienes reproducción automática
//  activada, que viene así por defecto) 🔊
// ============================================================

class WalkieTalkieForegroundService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_FLIPPY_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_FLIPPY_SERVICE"
        const val EXTRA_CHANNEL_NAME = "EXTRA_CHANNEL_NAME"
        private const val TAG = "WalkieTalkieFgSvc"

        /** true = la app está abierta en pantalla (el ViewModel reproduce). */
        @Volatile
        var uiInForeground: Boolean = false
            private set

        fun notifyUiForeground() { uiInForeground = true }
        fun notifyUiBackground() { uiInForeground = false }

        fun startService(context: Context, channelName: String = "144.800 MHz - General") {
            val intent = Intent(context, WalkieTalkieForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CHANNEL_NAME, channelName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, WalkieTalkieForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs by lazy { getSharedPreferences("flippy_prefs", Context.MODE_PRIVATE) }
    private val playQueue = Channel<String>(Channel.UNLIMITED) // rutas de audio por reproducir
    private var receiverStarted = false
    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // 🔊 Consumidor de la cola: reproduce los audios uno por uno
        scope.launch {
            for (path in playQueue) {
                playAudioBlocking(path)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val channelName =
                    intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "144.800 MHz - General"
                val notification = buildNotification(channelName)
                startForegroundSafely(notification)
                startBackgroundReceiver()
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    /** Si cierras la app del todo (deslizar), nos re-lanzamos solos 🔄 */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        runCatching {
            val restart = Intent(applicationContext, WalkieTalkieForegroundService::class.java)
                .apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restart)
            } else {
                applicationContext.startService(restart)
            }
            Log.i(TAG, "App cerrada → el servicio se re-agenda para seguir escuchando 📡")
        }.onFailure { Log.w(TAG, "No se pudo re-agendar al cerrar la app", it) }
    }

    // ============================================================
    //  🎧 Receptor de audios en segundo plano
    //  SOLO actúa cuando la app NO está en pantalla; si la app
    //  está abierta, el ViewModel se encarga (para no repetir).
    // ============================================================

    private fun startBackgroundReceiver() {
        if (receiverStarted) return
        if (!SupabaseRepository.isConfigured()) {
            Log.w(TAG, "Supabase no configurado: receptor en reposo")
            return
        }
        receiverStarted = true
        val myId = prefs.getString("flippy_id", "") ?: ""

        scope.launch {
            // Flujos listos ANTES de conectar (recomendación de supabase-kt)
            val incoming = SupabaseRepository.observeMessages()
            val pttEvents = SupabaseRepository.observePttEvents()
            val messageUpdates = SupabaseRepository.observeMessageUpdates()

            // 🔄 Conecta y REINTENTA cada 15s hasta conseguirlo: si el
            // servicio no conecta, con pantalla apagada no llega nada
            var connected = runCatching { SupabaseRepository.connectRealtime() }
                .getOrDefault(false)
            while (!connected) {
                delay(15_000)
                connected = runCatching { SupabaseRepository.connectRealtime() }
                    .getOrDefault(false)
            }
            Log.i(TAG, "Receptor en fondo conectado ✅ (mi ID: $myId)")

            // Sonido de notificación cuando el otro SUELTA el micrófono 🔔
            launch {
                pttEvents.collect { event ->
                    if (event.sender_flippy_id == myId) return@collect
                    if (uiInForeground) return@collect // la app abierta ya lo hace
                    if (event.event_type == "end") {
                        SoundEffects.playIncomingAlert()
                    }
                }
            }

            // Transcripciones que llegan después (solo log, sin UI aquí)
            launch {
                messageUpdates.collect { /* el texto queda en la nube e historial */ }
            }

            // Audios entrantes 🎙️ (encola si no estamos viendo la app)
            launch {
                incoming.collect { dto ->
                    if (dto.sender_flippy_id == myId) return@collect
                    if (uiInForeground) return@collect
                    if (!prefs.getBoolean("autoplay_incoming", true)) return@collect
                    val path = dto.audio_url
                    if (path.isNullOrBlank()) return@collect
                    Log.i(TAG, "📥 Audio entrante de ${dto.sender_name}: descargando y reproduciendo…")
                    playQueue.trySend(path)
                }
            }
        }
    }

    /** Reproduce un audio (de a uno por vez) manteniendo el CPU despierto. */
    private suspend fun playAudioBlocking(path: String) {
        val wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "flippy:incoming-audio")
        try {
            wakeLock.acquire(90_000L) // tope de seguridad
            val mp = MediaPlayer()
            player = mp
            var completed = false
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                runCatching {
                    mp.setDataSource(SupabaseRepository.publicAudioUrl(path))
                    mp.setOnPreparedListener {
                        it.start()
                        Log.i(TAG, "🔊 Reproduciendo audio entrante")
                    }
                    mp.setOnCompletionListener {
                        if (!completed) {
                            completed = true
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    mp.setOnErrorListener { _, what, _ ->
                        Log.w(TAG, "Error de reproducción $what")
                        if (!completed) {
                            completed = true
                            cont.resumeWith(Result.success(Unit))
                        }
                        true
                    }
                    mp.prepareAsync()
                }.onFailure {
                    if (!completed) {
                        completed = true
                        cont.resumeWith(Result.success(Unit))
                    }
                }
                cont.invokeOnCancellation { runCatching { mp.release() } }
            }
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo reproducir el audio entrante", e)
        } finally {
            runCatching { player?.release() }
            player = null
            if (wakeLock.isHeld) wakeLock.release()
            delay(150) // respiro entre audios encolados
        }
    }

    override fun onDestroy() {
        runCatching { player?.release() }
        player = null
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Starting a microphone-type foreground service requires RECORD_AUDIO on API 29+.
     * If the permission was denied, fall back to a plain foreground service so the
     * app does not crash on launch.
     */
    private fun startForegroundSafely(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                }
                return
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo iniciar FGS tipo microphone, reintentando sin tipo", e)
            }
        }
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e("Flippy", "No se pudo iniciar el servicio en primer plano", e)
        }
    }

    private fun buildNotification(channelName: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, FlippyApp.CHANNEL_ID)
            .setContentTitle("Flippy Walkie-Talkie Activo")
            .setContentText("En escucha: los audios sonarán aunque cierres la app 🔊")
            .setSmallIcon(R.drawable.ic_walkie_talkie)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
