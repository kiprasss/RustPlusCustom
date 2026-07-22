package com.example.rustplus

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import rustplus.Rustplus.AppEmpty
import rustplus.Rustplus.AppMessage
import rustplus.Rustplus.AppRequest

class RustPlusClient(
    private val serverIp: String,
    private val serverPort: Int,       // paprastai gameport + 67 -> pvz. 28082
    private val steamId: Long,
    private val playerToken: Int       // gaunamas iš FCM pairing pranešimo
) {
    companion object {
        // Bendras OkHttpClient visoms instancijoms - vengiama naujo thread pool
        // kūrimo kaskart paspaudus "Prisijungti prie serverio".
        private val sharedClient = OkHttpClient()
    }

    private var webSocket: WebSocket? = null
    private var seq = 0

    fun connect(onAlarm: (String) -> Unit) {
        // Uždarome bet kokį ankstesnį ryšį prieš atidarant naują - kitaip senas
        // WebSocket lieka atviras fone (resursų nutekėjimas), jei connect()
        // paspaudžiama pakartotinai.
        webSocket?.close(1000, null)

        val request = Request.Builder()
            .url("ws://$serverIp:$serverPort")
            .build()

        webSocket = sharedClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                android.util.Log.d("RustPlus", "Prisijungta prie serverio")
                sendGetInfo()
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // NOTE: AppMessage klasė sugeneruojama iš rustplus.proto (Gradle protobuf plugin).
                // Įsitikink, kad app/src/main/proto/rustplus.proto failas yra vietoje prieš build.
                val msg = AppMessage.parseFrom(bytes.toByteArray())
                if (msg.hasBroadcast() && msg.broadcast.hasEntityChanged()) {
                    val entity = msg.broadcast.entityChanged
                    if (entity.payload.value) {
                        onAlarm("Smart Alarm suveikė!")
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                android.util.Log.e("RustPlus", "Klaida: ${t.message}")
            }
        })
    }

    private fun sendGetInfo() {
        val request = AppRequest.newBuilder()
            .setSeq(++seq)
            .setPlayerId(steamId)
            .setPlayerToken(playerToken)
            .setGetInfo(AppEmpty.newBuilder().build())
            .build()
        webSocket?.send(ByteString.of(*request.toByteArray()))
    }

    fun disconnect() {
        webSocket?.close(1000, null)
        webSocket = null
    }
}
