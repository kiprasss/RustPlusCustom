package com.example.rustplus

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class SteamLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url?.contains("openid.claimed_id") == true) {
                    val steamId = Regex("id/(\\d+)").find(url)?.groupValues?.get(1)
                    if (steamId != null) {
                        getSharedPreferences("rustplus", MODE_PRIVATE)
                            .edit().putString("steam_id", steamId).apply()
                        finish()
                        return true
                    }
                }
                return false
            }
        }

        val loginUrl = "https://steamcommunity.com/openid/login" +
            "?openid.ns=http://specs.openid.net/auth/2.0" +
            "&openid.mode=checkid_setup" +
            "&openid.return_to=https://rustpluscustom.app/callback" +
            "&openid.realm=https://rustpluscustom.app" +
            "&openid.identity=http://specs.openid.net/auth/2.0/identifier_select" +
            "&openid.claimed_id=http://specs.openid.net/auth/2.0/identifier_select"

        webView.loadUrl(loginUrl)
    }
}
