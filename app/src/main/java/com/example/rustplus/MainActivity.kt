package com.example.rustplus

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var tvStatus: TextView

    private val debugHandler = Handler(Looper.getMainLooper())
    private var debugTriggered = false
    private val debugRunnable = Runnable {
        debugTriggered = true
        startActivity(Intent(this, DebugActivity::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("rustplus", MODE_PRIVATE)
        tvStatus = findViewById(R.id.tvStatus)

        findViewById<Button>(R.id.btnSteamLogin).setOnClickListener {
            startActivity(Intent(this, SteamLoginActivity::class.java))
        }

        val btnConnect = findViewById<Button>(R.id.btnConnect)

        // Palaikius šį mygtuką ~3 sekundes, atsidaro Debug Mode ekranas
        // (SharedPreferences reikšmės + rankinis server_ip/port/token įvedimas).
        btnConnect.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    debugTriggered = false
                    debugHandler.postDelayed(debugRunnable, 3000)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    debugHandler.removeCallbacks(debugRunnable)
                }
            }
            false // netrukdome įprastam onClick veikimui trumpiems paspaudimams
        }

        btnConnect.setOnClickListener {
            if (debugTriggered) {
                // Debug Mode jau atidarytas per ilgą paspaudimą - praleidžiame connect.
                return@setOnClickListener
            }
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
