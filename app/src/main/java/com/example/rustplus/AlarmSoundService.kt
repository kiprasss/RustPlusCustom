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

    companion object {
        // Naudojama tiek pranešimo "Sustabdyti" mygtuko, tiek programos vidaus
        // mygtuko - abu siunčia tą patį veiksmą, tad aliarmas sustabdomas
        // vienodai nepriklausomai nuo to, ar programa atidaryta, ar fone/uždaryta.
        const val ACTION_STOP = "com.example.rustplus.ACTION_STOP"
        private const val NOTIF_ID = 1
        private const val CHANNEL_ID = "alarm_channel"
    }

    private var mediaPlayer: MediaPlayer? = null
    private val stopHandler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stopAlarm() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            return START_NOT_STICKY
        }

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

        // "Sustabdyti" veiksmas pranešime - paspaudus, PendingIntent siunčia
        // ACTION_STOP atgal į šį patį servisą (veikia net kai programa uždaryta).
        val stopIntent = Intent(this, AlarmSoundService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            // setOngoing(true): pranešimo nebegalima tiesiog nubraukti pirštu -
            // anksčiau tai buvo galima, o garsas toliau grodavo be jokio matomo
            // pranešimo. Dabar vienintelis būdas sustabdyti - "Sustabdyti" mygtukas
            // (arba 30s automatinis laikmatis), tad garsas ir pranešimas visada
            // lieka sinchronizuoti.
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Sustabdyti", stopPendingIntent)
            .setFullScreenIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                ), true
            )
            .build()
        startForeground(NOTIF_ID, notif)

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

    private fun stopAlarm() {
        stopHandler.removeCallbacks(stopRunnable)
        try {
            mediaPlayer?.stop()
        } catch (e: IllegalStateException) {
            // jau sustabdytas / dar nepaleistas - ignoruojame
        }
        mediaPlayer?.release()
        mediaPlayer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
                CHANNEL_ID, "Smart Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Rust Smart Alarm pranešimai"
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
}
