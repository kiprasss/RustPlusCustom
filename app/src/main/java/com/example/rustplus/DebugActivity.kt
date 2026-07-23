package com.example.rustplus

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class DebugActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var tvInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        prefs = getSharedPreferences("rustplus", MODE_PRIVATE)
        tvInfo = findViewById(R.id.tvDebugInfo)

        val etTestAlarmText = findViewById<EditText>(R.id.etTestAlarmText)
        val etIp = findViewById<EditText>(R.id.etServerIp)
        val etPort = findViewById<EditText>(R.id.etServerPort)
        val etToken = findViewById<EditText>(R.id.etPlayerToken)

        etIp.setText(prefs.getString("server_ip", ""))
        etPort.setText(prefs.getInt("server_port", 28082).toString())
        etToken.setText(prefs.getInt("player_token", 0).toString())

        refreshInfo()

        // Testuoja PILNĄ aliarmo grandinę (foreground service, notification, garsas)
        // lygiai taip pat, kaip tai vyktų gavus tikrą Smart Alarm signalą iš serverio -
        // bet be jokio realaus WebSocket/FCM ryšio.
        findViewById<Button>(R.id.btnTestAlarm).setOnClickListener {
            val testMsg = etTestAlarmText.text.toString().trim()
                .ifEmpty { "Testinis Smart Alarm pranešimas" }
            ContextCompat.startForegroundService(
                this,
                Intent(this, AlarmSoundService::class.java)
                    .putExtra("title", "🚨 TEST Smart Alarm")
                    .putExtra("body", testMsg)
            )
            Toast.makeText(this, "Aliarmas paleistas (testinis)", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnStopAlarm).setOnClickListener {
            startService(
                Intent(this, AlarmSoundService::class.java).apply { action = AlarmSoundService.ACTION_STOP }
            )
            Toast.makeText(this, "Aliarmas sustabdytas", Toast.LENGTH_SHORT).show()
        }

        // Simuliuoja sėkmingą "pairing" atsakymą (tarsi FCM būtų atsiuntęs
        // server_ip/port/playerToken) - MainActivity po to rodys "Serveris: susietas".
        findViewById<Button>(R.id.btnSimulatePairing).setOnClickListener {
            prefs.edit()
                .putString("server_ip", "127.0.0.1")
                .putInt("server_port", 28082)
                .putInt("player_token", 123456789)
                .apply()
            etIp.setText("127.0.0.1")
            etPort.setText("28082")
            etToken.setText("123456789")
            Toast.makeText(this, "Pairing simuliuotas - dabar 'Susieta'", Toast.LENGTH_SHORT).show()
            refreshInfo()
        }

        findViewById<Button>(R.id.btnSimulateDisconnect).setOnClickListener {
            prefs.edit()
                .remove("server_ip")
                .remove("server_port")
                .remove("player_token")
                .apply()
            etIp.setText("")
            etPort.setText("28082")
            etToken.setText("0")
            Toast.makeText(this, "Atsijungimas simuliuotas", Toast.LENGTH_SHORT).show()
            refreshInfo()
        }

        findViewById<Button>(R.id.btnSaveManual).setOnClickListener {
            val ip = etIp.text.toString().trim()
            val port = etPort.text.toString().trim().toIntOrNull() ?: 28082
            val token = etToken.text.toString().trim().toIntOrNull() ?: 0

            prefs.edit()
                .putString("server_ip", ip.ifEmpty { null })
                .putInt("server_port", port)
                .putInt("player_token", token)
                .apply()

            Toast.makeText(this, "Rankiniai duomenys išsaugoti", Toast.LENGTH_SHORT).show()
            refreshInfo()
        }

        findViewById<Button>(R.id.btnClearAll).setOnClickListener {
            prefs.edit().clear().apply()
            etIp.setText("")
            etPort.setText("28082")
            etToken.setText("0")
            Toast.makeText(this, "Visi duomenys išvalyti", Toast.LENGTH_SHORT).show()
            refreshInfo()
        }
    }

    private fun refreshInfo() {
        val fcmToken = prefs.getString("fcm_token", null)
        val fcmShort = if (fcmToken != null) fcmToken.take(24) + "..." else "—"

        tvInfo.text = "BŪSENA\n\n" +
            "steam_id: ${prefs.getString("steam_id", "—")}\n" +
            "fcm_token: $fcmShort\n" +
            "player_token: ${prefs.getInt("player_token", 0)}\n" +
            "server_ip: ${prefs.getString("server_ip", "—")}\n" +
            "server_port: ${prefs.getInt("server_port", 28082)}\n\n" +
            "Paskutinis tikras FCM pranešimas (žaliaviniai duomenys):\n" +
            (prefs.getString("last_fcm_raw", "— dar negauta nei vieno —") ?: "—")
    }
}
