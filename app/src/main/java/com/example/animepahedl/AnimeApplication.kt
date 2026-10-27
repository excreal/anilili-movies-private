package com.example.animepahedl

import android.app.Application
import android.webkit.WebSettings

class AnimeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugTrace.initialize(this)
        // Keep HTTP and WebView on the same UA across launches, including service restarts.
        val preferences = getSharedPreferences("browser-session", MODE_PRIVATE)
        AnimeNetwork.userAgent = preferences.getString("user-agent", null)
            ?: WebSettings.getDefaultUserAgent(this).also {
                preferences.edit().putString("user-agent", it).apply()
            }
    }
}
