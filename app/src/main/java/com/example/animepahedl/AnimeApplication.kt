package com.example.animepahedl

import androidx.core.content.edit
import android.app.Application
import android.webkit.WebSettings

class AnimeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DownloadStore.initialize(this)
        DebugTrace.initialize(this)
        // Keep HTTP and WebView on the same UA across launches, including service restarts.
        val preferences = getSharedPreferences("browser-session", MODE_PRIVATE)
        AnimeNetwork.userAgent = preferences.getString("user-agent", null)
            ?: runCatching { WebSettings.getDefaultUserAgent(this) }.getOrDefault(AnimeNetwork.userAgent).also {
                preferences.edit { putString("user-agent", it) }
            }
    }
}
