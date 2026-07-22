package com.example.rustplus

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("rustplus", MODE_PRIVATE)
        tvStatus = findViewById(R.id.tvStatus)

        findViewById<android.widget.Button>(R.id.btnSteamLogin).setOnClickListener {
            startActivity(Intent(this, SteamLoginActivity::class.java))
        }

        findViewById<android.widget.Button>(R.id.btnConnect).setOnClickListener {
            val steamId = prefs.getString("steam_id", null)?.toLongOrNull()
            if (steamId == null) {
                Toast.makeText(this, "Pirma prisijunkite per Steam", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val ip = prefs.getString("server_ip", null)
            if (ip == null) {
                Toast.makeText(
                    this,
                    "Dar nesusieta su serveriu. Žaidime: ESC → Rust+ → Pair with Server",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            val token = prefs.getInt("player_token", 0)
            val port = prefs.getInt("server_port", 28082)

            Toast.makeText(this, "Jungiamasi prie $ip:$port...", Toast.LENGTH_SHORT).show()
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

    override fun onResume() {
        super.onResume()
        // Atnaujiname būseną kaskart grįžus į ekraną (pvz. po Steam prisijungimo),
        // kad matytumėte, ar steam_id/pairing jau išsaugoti - be šito ekranas
        // atrodydavo "nieko nevyksta" net kai prisijungimas realiai pavykdavo.
        updateStatus()
    }

    private fun updateStatus() {
        val steamId = prefs.getString("steam_id", null)
        val serverIp = prefs.getString("server_ip", null)
        val steamPart = if (steamId != null) "Steam: prisijungta ($steamId)" else "Steam: neprisijungta"
        val pairPart = if (serverIp != null) "Serveris: susietas ($serverIp)" else "Serveris: nesusietas"
        tvStatus.text = "$steamPart\n$pairPart"
    }
}
