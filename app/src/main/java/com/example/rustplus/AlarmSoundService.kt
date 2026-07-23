package com.example.rustplus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class AlarmSoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val stopHandler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stopSelf() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("title") ?: "SMART ALARM"
        val body = intent?.getStringExtra("body") ?: "Kažkas užpuolė bazę!"

        // Jei ankstesnis aliarmas dar skambėjo (naujas Smart Alarm atėjo per <30s),
        // sustabdome jo laikmatį ir MediaPlayer - kitaip senas laikmatis gali per
        // anksti nutraukti NAUJĄ aliarmą, arba senas MediaPlayer nutekės.
        stopHandler.removeCallbacks(stopRunnable)
        try {
            mediaPlayer?.stop()
        } catch (e: IllegalStateException) {
            // jau sustabdytas / dar nepaleistas - ignoruojame
        }
        mediaPlayer?.release()
        mediaPlayer = null

        createChannel()
        val notif = NotificationCompat.Builder(this, "alarm_channel")
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                ), true
            )
            .build()
        startForeground(1, notif)

        // Reikalauja app/src/main/res/raw/alarm.mp3 failo
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm)?.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            start()
        }

        stopHandler.postDelayed(stopRunnable, 30_000)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopHandler.removeCallbacks(stopRunnable)
        try {
            mediaPlayer?.stop()
        } catch (e: IllegalStateException) {
            // jau sustabdytas / dar nepaleistas - ignoruojame
        }
        mediaPlayer?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                "alarm_channel", "Smart Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Rust Smart Alarm pranešimai"
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
}
