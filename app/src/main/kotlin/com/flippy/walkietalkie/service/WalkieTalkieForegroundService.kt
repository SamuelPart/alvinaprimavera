package com.flippy.walkietalkie.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.flippy.walkietalkie.FlippyApp
import com.flippy.walkietalkie.MainActivity
import com.flippy.walkietalkie.R

class WalkieTalkieForegroundService : Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_FLIPPY_SERVICE"
        const val ACTION_STOP = "ACTION_STOP_FLIPPY_SERVICE"
        const val EXTRA_CHANNEL_NAME = "EXTRA_CHANNEL_NAME"
        private const val TAG = "WalkieTalkieFgSvc"

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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "144.800 MHz - General"
                val notification = buildNotification(channelName)
                startForegroundSafely(notification)
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
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
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
                return
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo iniciar FGS tipo microphone, reintentando sin tipo", e)
            }
        }
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo iniciar el servicio en primer plano", e)
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
            .setContentText("Sintonizado: $channelName • En escucha en segundo plano")
            .setSmallIcon(R.drawable.ic_walkie_talkie)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
