package com.example.rustplus

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("rustplus", MODE_PRIVATE)

        findViewById<android.widget.Button>(R.id.btnSteamLogin).setOnClickListener {
            startActivity(Intent(this, SteamLoginActivity::class.java))
        }

        findViewById<android.widget.Button>(R.id.btnConnect).setOnClickListener {
            val steamId = prefs.getString("steam_id", null)?.toLongOrNull() ?: return@setOnClickListener
            val token = prefs.getInt("player_token", 0)
            val ip = prefs.getString("server_ip", null) ?: return@setOnClickListener
            val port = prefs.getInt("server_port", 28082)

            val client = RustPlusClient(ip, port, steamId, token)
            client.connect { alarmText ->
                startService(
                    Intent(this, AlarmSoundService::class.java)
                        .putExtra("title", "Smart Alarm")
                        .putExtra("body", alarmText)
                )
            }
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                prefs.edit().putString("fcm_token", task.result).apply()
            }
        }
    }
}
