package com.example.animepahedl

import android.app.Notification
import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import java.io.File
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import com.google.gson.Gson

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object OfflineDownloads {
    private var instance: DownloadManager? = null
    private var segmentExecutor: ThreadPoolExecutor? = null

    /** Progress and state changes from Media3, consumed by the UI instead of a polling loop. */
    val updates = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)

    @Synchronized
    fun setConnections(count: Int) {
        val limit = count.coerceIn(2, 12)
        segmentExecutor?.let { pool ->
            if (limit > pool.maximumPoolSize) { pool.maximumPoolSize = limit; pool.corePoolSize = limit }
            else { pool.corePoolSize = limit; pool.maximumPoolSize = limit }
        }
    }
    lateinit var cache: SimpleCache
        private set

    internal fun retryPending(context: Context, manager: DownloadManager) {
        if (manager.downloadsPaused) return
        val retries = context.getSharedPreferences("download-retries", Context.MODE_PRIVATE)
        val requests = mutableListOf<androidx.media3.exoplayer.offline.DownloadRequest>()
        manager.downloadIndex.getDownloads(Download.STATE_FAILED).use { cursor ->
            while (cursor.moveToNext()) {
                val download = cursor.download
                if (retries.getBoolean(download.request.id, false) && download.stopReason == Download.STOP_REASON_NONE)
                    requests += download.request
            }
        }
        requests.forEach { manager.addDownload(it) }
    }

    @Synchronized
    fun manager(context: Context): DownloadManager {
        instance?.let { return it }
        val app = context.applicationContext
        val database = StandaloneDatabaseProvider(app)
        val legacy = File(app.filesDir, "offline-media")
        // Retain pre-update downloads until they have been exported successfully.
        val directory = if (legacy.exists() && legacy.walkTopDown().any { it.extension == "exo" }) legacy
            else File(app.getExternalFilesDir(null) ?: app.filesDir, "hls-temp")
        cache = SimpleCache(directory, NoOpCacheEvictor(), database)
        val factory = OkHttpDataSource.Factory(AnimeNetwork.mediaClient)
            .setDefaultRequestProperties(mapOf("Referer" to "https://kwik.cx/"))
        val connections = AppSettings(app).connections
        val executor = ThreadPoolExecutor(connections, connections, 30, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>())
            .apply { allowCoreThreadTimeOut(true) }
        segmentExecutor = executor
        return DownloadManager(app, database, cache, factory, executor).also {
            val retries = app.getSharedPreferences("download-retries", Context.MODE_PRIVATE)
            it.addListener(object : DownloadManager.Listener {
                override fun onInitialized(manager: DownloadManager) {
                    retryPending(app, manager)
                    updates.tryEmit(Unit)
                }
                override fun onDownloadChanged(manager: DownloadManager, download: Download, finalException: Exception?) {
                    val challenge = finalException?.let { error ->
                        generateSequence<Throwable>(error) { it.cause }.filterIsInstance<VerificationRequired>().firstOrNull()
                    }
                    if (challenge != null) {
                        val event = MediaVerification(download.request.id, challenge.url, challenge.browserUrl, System.currentTimeMillis())
                        app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).edit()
                            .putString(event.id, Gson().toJson(event)).apply()
                        DebugTrace.event("media_challenge", "url" to DebugTrace.safeUrl(challenge.url))
                    }
                    if (finalException != null) DebugTrace.event("media_failed", "type" to finalException.javaClass.simpleName,
                        "url" to DebugTrace.safeUrl(download.request.uri.toString()))
                    // Connectivity requirements handle a disconnected device. Retry transient I/O failures
                    // too (for example a router with no internet), retaining every cached segment.
                    val causes = finalException?.let { generateSequence<Throwable>(it) { error -> error.cause }.toList() }.orEmpty()
                    val http = causes.filterIsInstance<androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException>().firstOrNull()
                    if (download.state == Download.STATE_FAILED && challenge == null &&
                        causes.any { it is java.io.IOException } && (http == null || http.responseCode == 408 || http.responseCode == 429 || http.responseCode >= 500)) {
                        retries.edit().putBoolean(download.request.id, true).apply()
                        DownloadRetryService.schedule(app)
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            val current = manager.downloadIndex.getDownload(download.request.id)
                            if (current?.state == Download.STATE_FAILED && current.stopReason == Download.STOP_REASON_NONE && !manager.downloadsPaused)
                                manager.addDownload(current.request)
                        }, 30_000)
                    }
                    if (download.state == Download.STATE_COMPLETED || download.state == Download.STATE_REMOVING ||
                        (download.state == Download.STATE_FAILED && (challenge != null || (http != null && http.responseCode in 400..499 && http.responseCode != 408 && http.responseCode != 429))))
                        retries.edit().remove(download.request.id).apply()
                    if (download.state == Download.STATE_COMPLETED) {
                        app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).edit().remove(download.request.id).apply()
                        if (!Exports.all(app).containsKey(download.request.id))
                            Exports.prepare(app, download.request.id, String(download.request.data))
                        Exports.start(app)
                    }
                    updates.tryEmit(Unit)
                }
            })
            it.minRetryCount = 10
            it.maxParallelDownloads = 2
            it.resumeDownloads()
            instance = it
        }
    }
}

internal data class MediaVerification(val id: String, val url: String, val browserUrl: String, val time: Long)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class OfflineDownloadService : DownloadService(1, 1000L, "downloads", R.string.download_channel, 0) {
    private val wakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as android.os.PowerManager)
            .newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "$packageName:offline-downloads")
            .apply { setReferenceCounted(false) }
    }

    override fun onCreate() {
        super.onCreate()
        // A foreground service alone does not keep the CPU awake on aggressive OEM builds.
        if (!wakeLock.isHeld) wakeLock.acquire()
    }

    override fun onDestroy() {
        if (wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
    }

    override fun getDownloadManager(): DownloadManager = OfflineDownloads.manager(this)
    override fun getScheduler(): Scheduler = androidx.media3.exoplayer.scheduler.PlatformScheduler(this, 10)
    override fun onTimeout(startId: Int, fgsType: Int) {
        // Android 15+ limits data-sync foreground-service runtime.
        OfflineDownloads.manager(this).pauseDownloads()
        stopSelf()
    }
    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification =
        DownloadNotificationHelper(this, "downloads").buildProgressNotification(
            this, android.R.drawable.stat_sys_download, null, "Animepahe DL · Offline downloads", downloads, notMetRequirements
        )
}

/** A persisted job survives process death between a transient failure and its delayed retry. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class DownloadRetryService : android.app.job.JobService() {
    override fun onStartJob(params: android.app.job.JobParameters): Boolean {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val result = runCatching {
                DownloadService.startForeground(this, OfflineDownloadService::class.java)
                val manager = OfflineDownloads.manager(this)
                if (manager.isInitialized) OfflineDownloads.retryPending(this, manager)
            }
            result.onFailure { DebugTrace.event("download_retry_deferred", "reason" to it.message) }
            jobFinished(params, result.isFailure)
        }
        return true
    }
    override fun onStopJob(params: android.app.job.JobParameters) = true

    companion object {
        fun schedule(context: Context) {
            val job = android.app.job.JobInfo.Builder(11, android.content.ComponentName(context, DownloadRetryService::class.java))
                .setRequiredNetworkType(android.app.job.JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(30_000).setPersisted(true)
                .setBackoffCriteria(30_000, android.app.job.JobInfo.BACKOFF_POLICY_EXPONENTIAL).build()
            context.getSystemService(android.app.job.JobScheduler::class.java).schedule(job)
        }
    }
}
