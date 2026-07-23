package com.example.rustplus

import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d("FCM", "Naujas token: $token")
        getSharedPreferences("rustplus", MODE_PRIVATE)
            .edit().putString("fcm_token", token).apply()
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        val data = msg.data

        // Debug: išsaugome paskutinio FCM pranešimo žaliavinius duomenis, kad
        // Debug Mode ekrane matytumėte tikrus laukų pavadinimus iš realaus serverio.
        getSharedPreferences("rustplus", MODE_PRIVATE).edit()
            .putString("last_fcm_raw", data.toString())
            .apply()

        val channelId = data["channelId"] ?: return

        when (channelId) {
            "alarm" -> {
                val title = data["title"] ?: "Rust Smart Alarm"
                val body = data["message"] ?: "Alarm buvo aktyvuotas"

                ContextCompat.startForegroundService(this, Intent(this, AlarmSoundService::class.java).apply {
                    putExtra("title", title)
                    putExtra("body", body)
                })
            }
            "pairing" -> {
                val playerToken = data["playerToken"]?.toIntOrNull() ?: return
                val ip = data["ip"]
                val port = data["port"]?.toIntOrNull()
                getSharedPreferences("rustplus", MODE_PRIVATE).edit()
                    .putInt("player_token", playerToken)
                    .putString("server_ip", ip)
                    .putInt("server_port", port ?: 28082)
                    .apply()
            }
        }
    }
}
