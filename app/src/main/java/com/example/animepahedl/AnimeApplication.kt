package com.example.animepahedl

import androidx.core.content.edit
import android.app.Application
import android.webkit.WebSettings
import kotlinx.coroutines.*

class AnimeApplication : Application(), coil.ImageLoaderFactory {
    override fun newImageLoader(): coil.ImageLoader = coil.ImageLoader.Builder(this)
        .okHttpClient(AnimeNetwork.client)
        .memoryCache { coil.memory.MemoryCache.Builder(this).maxSizePercent(0.08).build() }.build()
    private val recoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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
        recoveryScope.launch {
            try {
                DownloadStore.awaitReady()
                if (hasOutputFolderAccess(this@AnimeApplication) && DownloadStore.operations("folder-access")["transfers"] != null)
                    reconcileExistingVideos(this@AnimeApplication)
                PreparationJobs.recover(this@AnimeApplication)
            }
            catch (_: Exception) { DebugTrace.event("preparation_recovery_deferred") }
        }
    }
}
