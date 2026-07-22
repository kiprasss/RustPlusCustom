package com.example.rustplus

import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class SteamLoginActivity : AppCompatActivity() {

    private val returnUrl = "https://rustpluscustom.app/callback"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // NOTE: naudojame Uri.parse().getQueryParameter(), NE regex ant žaliavinio
                // string'o, nes Steam grąžina URL su url-encoded '/' (%2F) parametruose –
                // paprastas regex "id/(\d+)" to neatpažįsta.
                if (url != null && url.startsWith(returnUrl)) {
                    val claimedId = Uri.parse(url).getQueryParameter("openid.claimed_id")
                    val steamId = claimedId?.let { Regex("(\\d+)$").find(it)?.value }
                    if (steamId != null) {
                        getSharedPreferences("rustplus", MODE_PRIVATE)
                            .edit().putString("steam_id", steamId).apply()
                    }
                    finish()
                    return true
                }
                return false
            }
        }

        val loginUrl = "https://steamcommunity.com/openid/login" +
            "?openid.ns=http://specs.openid.net/auth/2.0" +
            "&openid.mode=checkid_setup" +
            "&openid.return_to=$returnUrl" +
            "&openid.realm=https://rustpluscustom.app" +
            "&openid.identity=http://specs.openid.net/auth/2.0/identifier_select" +
            "&openid.claimed_id=http://specs.openid.net/auth/2.0/identifier_select"

        webView.loadUrl(loginUrl)
    }
}
