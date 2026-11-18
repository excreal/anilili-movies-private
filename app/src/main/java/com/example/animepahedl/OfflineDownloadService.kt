package com.example.animepahedl

import androidx.core.content.edit
import android.app.Notification
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MimeTypes
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import androidx.core.app.NotificationCompat
import android.text.format.Formatter
import java.io.File
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object OfflineDownloads {
    private var instance: DownloadManager? = null
    private var segmentExecutor: ThreadPoolExecutor? = null
    private val retryLock = kotlinx.coroutines.sync.Mutex()
    private val resolvingLinks = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    internal fun isGettingLink(id: String): Boolean = id in resolvingLinks
    private val hlsSamples = java.util.concurrent.ConcurrentHashMap<String, HlsTransferSample>()
    internal fun hlsSample(id: String): HlsTransferSample? = hlsSamples[id]

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
    @Synchronized
    fun setSimultaneousDownloads(count: Int) {
        instance?.maxParallelDownloads = count.coerceIn(1, 6)
    }
    lateinit var cache: SimpleCache
        private set

    /** Called on IO before UI startup; services retain a synchronized fallback. */
    @Synchronized
    internal fun prepareCache(context: Context) {
        if (::cache.isInitialized) return
        val app = context.applicationContext
        val database = StandaloneDatabaseProvider(app)
        val legacy = File(app.filesDir, "offline-media")
        // Retain pre-update downloads until they have been exported successfully.
        val directory = if (legacy.exists() && legacy.walkTopDown().any { it.extension == "exo" }) legacy
            else File(app.getExternalFilesDir(null) ?: app.filesDir, "hls-temp")
        cache = SimpleCache(directory, NoOpCacheEvictor(), database)
    }

    internal suspend fun retryPending(context: Context, manager: DownloadManager) = retryLock.withLock {
        if (manager.downloadsPaused || !hasOutputFolderAccess(context) || DownloadStore.operationValue("transfers", "folder-access") != null) return@withLock
        val retries = context.getSharedPreferences("download-retries", Context.MODE_PRIVATE)
        val requests = withContext(Dispatchers.IO) {
            manager.downloadIndex.getDownloads(Download.STATE_FAILED).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val download = cursor.download
                        if (download.stopReason == Download.STOP_REASON_NONE) add(download.request)
                    }
                }
            }
        }
        val records = Exports.all(context)
        val eligible = requests.filter { request ->
            val id = request.id
            val record = records[id]
            !Exports.deleting(id) && retries.getBoolean(id, false) && retries.getInt("attempts:$id", 0) < MAX_TRANSFER_RESTARTS &&
                record?.uri?.isEmpty() == true && downloadRecoveryEligibility(record).transferControllable
        }
        if (eligible.isEmpty() || manager.downloadsPaused) return@withLock
        DownloadService.startForeground(context, OfflineDownloadService::class.java)
        eligible.forEach { request ->
            val id = request.id
            retries.edit { remove(id); putInt("attempts:$id", retries.getInt("attempts:$id", 0) + 1) }
            manager.addDownload(request)
        }
    }

    @Synchronized
    fun manager(context: Context): DownloadManager {
        instance?.let { return it }
        val app = context.applicationContext
        val database = StandaloneDatabaseProvider(app)
        if (!::cache.isInitialized) prepareCache(app)
        val transferListener = object : androidx.media3.datasource.TransferListener {
            private val last = java.util.concurrent.atomic.AtomicLong(0)
            override fun onTransferInitializing(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean) = Unit
            override fun onTransferStart(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean) = Unit
            override fun onBytesTransferred(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean, bytesTransferred: Int) {
                if (isNetwork) DownloadSpeed.add(bytesTransferred)
                val now = android.os.SystemClock.elapsedRealtime()
                val previous = last.get()
                if (now - previous >= 750 && last.compareAndSet(previous, now)) updates.tryEmit(Unit)
            }
            override fun onTransferEnd(source: androidx.media3.datasource.DataSource, dataSpec: androidx.media3.datasource.DataSpec, isNetwork: Boolean) = Unit
        }
        val connections = AppSettings(app).connections
        val executor = ThreadPoolExecutor(connections, connections, 30, TimeUnit.SECONDS, java.util.concurrent.ArrayBlockingQueue<Runnable>(48), ThreadPoolExecutor.CallerRunsPolicy())
            .apply { allowCoreThreadTimeOut(true) }
        segmentExecutor = executor
        val downloaderFactory = androidx.media3.exoplayer.offline.DownloaderFactory { request ->
            fun createTransfer(renew: Boolean): androidx.media3.exoplayer.offline.Downloader {
                runBlocking { DownloadStore.awaitReady() }
                if (!hasOutputFolderAccess(app) || DownloadStore.operations("folder-access")["transfers"] != null)
                    error("Choose downloads folder")
                val record = requireNotNull(Exports.all(app)[request.id]) { "This download record is missing. Retry to resolve this episode again." }
                val identity = requireNotNull(record.identity?.takeIf { it.isSiteDownload }) { "Video link unavailable" }
                val cookies = SiteDownloadCookieStore(app)
                val media = if (!renew && record.media != null) record.media else {
                    val locator = requireNotNull(identity.locator?.takeIf(String::isNotBlank)) { "Video link unavailable" }
                    resolvingLinks.add(request.id)
                    updates.tryEmit(Unit)
                    try {
                        runBlocking {
                            val resolved = cookies.applyTo(appSiteDownloadRepository(cookieHeader = cookies::headerFor)
                                .resolve(locator, identity.quality, cookies.headerFor(locator), includeSubtitles = false))
                            check(!Exports.deleting(request.id)) { "Download removed" }
                            val current = requireNotNull(Exports.all(app)[request.id]) { "Download removed" }
                            val quality = identity.quality.takeIf {
                                resolved.isHls() && it.endsWith("p") && it.dropLast(1).toIntOrNull() != null
                            } ?: resolved.quality
                            if (resolved.url != current.media?.url) hlsSamples.remove(request.id)
                            Exports.put(app, request.id, current.copy(quality = quality, audio = resolved.audio, media = resolved))
                            DownloadStore.flush()
                            resolved
                        }
                    } finally {
                        resolvingLinks.remove(request.id)
                        updates.tryEmit(Unit)
                    }
                }
                val actualRequest = request.forResolvedMedia(media)
                val upstream = OkHttpDataSource.Factory(MediaRequests.client(media, cookies::headerFor))
                    .setTransferListener(transferListener)
                    .setContentTypePredicate { normalizedMediaType(it) !in setOf("text/html", "application/xhtml+xml") }
                // Resolve headers for every manifest, segment, and encryption-key URI. HLS resources
                // commonly move across CDN hosts, so applying one static header map leaks or misses
                // provider-specific headers after relative URLs and redirects are resolved.
                val resolving = ResolvingDataSource.Factory(upstream) { dataSpec ->
                    dataSpec.withRequestHeaders(mediaRequestHeaders(media, dataSpec.uri.toString()))
                }
                val factory = androidx.media3.datasource.cache.CacheDataSource.Factory().setCache(cache)
                    .setUpstreamDataSourceFactory(resolving)
                return if (media.isHls()) {
                    check(File(app.applicationInfo.nativeLibraryDir, "libffmpeg_exec.so").exists()) { "HLS requires an ARM64 device" }
                    val quality = Exports.all(app)[request.id]?.quality ?: media.quality
                    selectedHlsDownloader(actualRequest, quality, factory, executor) { sample ->
                        if (hlsSamples.put(request.id, sample) != sample) updates.tryEmit(Unit)
                    }
                } else androidx.media3.exoplayer.offline.DefaultDownloaderFactory(factory, executor).createDownloader(actualRequest)
            }
            DeferredDownloader(create = { createTransfer(false) }, refresh = { createTransfer(true) }, expired = ::isExpiredMediaResponse, removeCached = {
                // Cache removal never needs a live provider session or output-folder access.
                val records = Exports.all(app)
                val actual = request.forResolvedMedia(records[request.id]?.media)
                val stillNeeded = isHlsMimeType(actual.mimeType) && records.any { (id, item) ->
                    id != request.id && item.uri.isEmpty() && !Exports.deleting(id) && item.media?.url == actual.uri.toString()
                }
                if (!stillNeeded && actual.mimeType != null) {
                    val factory = androidx.media3.datasource.cache.CacheDataSource.Factory().setCache(cache)
                    androidx.media3.exoplayer.offline.DefaultDownloaderFactory(factory, executor)
                        .createDownloader(actual).remove()
                }
            })
        }
        return DownloadManager(app, androidx.media3.exoplayer.offline.DefaultDownloadIndex(database), downloaderFactory).also {
            val retries = app.getSharedPreferences("download-retries", Context.MODE_PRIVATE)
            it.addListener(object : DownloadManager.Listener {
                override fun onDownloadsPausedChanged(manager: DownloadManager, downloadsPaused: Boolean) {
                    if (!downloadsPaused && (!hasOutputFolderAccess(app) || DownloadStore.operations("folder-access")["transfers"] != null)) manager.pauseDownloads()
                }
                override fun onInitialized(manager: DownloadManager) {
                    if (retries.all.values.any { it == true }) DownloadRetryService.schedule(app)
                    updates.tryEmit(Unit)
                }
                override fun onDownloadChanged(manager: DownloadManager, download: Download, finalException: Exception?) {
                    DebugTrace.event("download_state", "id" to download.request.id.hashCode().toUInt().toString(),
                        "state" to download.state, "bytes" to download.bytesDownloaded, "expectedBytes" to download.contentLength,
                        "stopReason" to download.stopReason, "failureReason" to download.failureReason)
                    val challenge = finalException?.let { error ->
                        generateSequence<Throwable>(error) { it.cause }.filterIsInstance<VerificationRequired>().firstOrNull()
                    }
                    if (challenge != null) {
                        val event = MediaVerification(download.request.id, challenge.url, challenge.browserUrl, System.currentTimeMillis())
                        app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).edit { putString(event.id, Gson().toJson(event)) }
                        DebugTrace.event("media_challenge", "url" to DebugTrace.safeUrl(challenge.url))
                    }
                    if (finalException != null) DebugTrace.event("media_failed", "type" to finalException.javaClass.simpleName,
                        "url" to DebugTrace.safeUrl(download.request.uri.toString()))
                    // Connectivity requirements handle a disconnected device. Retry transient I/O failures
                    // too (for example a router with no internet), retaining every cached segment.
                    val causes = finalException?.let { generateSequence<Throwable>(it) { error -> error.cause }.toList() }.orEmpty()
                    val http = causes.filterIsInstance<androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException>().firstOrNull()
                    val id = download.request.id
                    if (download.state == Download.STATE_FAILED && causes.any { it is MediaLinkExpired }) {
                        DownloadStore.operation(id, "expired-media", "1")
                    } else if (download.state != Download.STATE_FAILED && DownloadStore.operationValue(id, "expired-media") != null) {
                        DownloadStore.operation(id, "expired-media", null)
                    }
                    if (download.state == Download.STATE_FAILED) {
                        val retry = challenge == null && !Exports.deleting(id) &&
                            causes.none { it is androidx.media3.datasource.HttpDataSource.InvalidContentTypeException } &&
                            causes.any { it is java.io.IOException } && isTransientTransferHttpStatus(http?.responseCode) &&
                            retries.getInt("attempts:$id", 0) < MAX_TRANSFER_RESTARTS
                        retries.edit { if (retry) putBoolean(id, true) else remove(id) }
                        if (retry) DownloadRetryService.schedule(app)
                    }
                    if (download.state == Download.STATE_COMPLETED || download.state == Download.STATE_REMOVING) {
                        retries.edit { remove(id); remove("attempts:$id") }
                        hlsSamples.remove(id)
                    }
                    if (download.state == Download.STATE_COMPLETED && !Exports.deleting(download.request.id)) {
                        app.getSharedPreferences("media-verification", Context.MODE_PRIVATE).edit { remove(download.request.id) }
                        if (!Exports.all(app).containsKey(download.request.id))
                            Exports.prepare(app, download.request.id, String(download.request.data))
                        Exports.start(app)
                    }
                    updates.tryEmit(Unit)
                }
            })
            it.setRequirements(androidx.media3.exoplayer.scheduler.Requirements(if (AppSettings(app).unmeteredOnly) androidx.media3.exoplayer.scheduler.Requirements.NETWORK_UNMETERED else androidx.media3.exoplayer.scheduler.Requirements.NETWORK))
            it.minRetryCount = 10
            it.maxParallelDownloads = AppSettings(app).simultaneousDownloads
            if (hasOutputFolderAccess(app) && DownloadStore.operations("folder-access")["transfers"] == null) it.resumeDownloads() else {
                it.pauseDownloads()
                DownloadStore.operation("transfers", "folder-access", "1")
            }
            instance = it
        }
    }
}

internal data class MediaVerification(val id: String, val url: String, val browserUrl: String, val time: Long)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun DownloadRequest.forResolvedMedia(media: ResolvedMedia?): DownloadRequest {
    if (media == null) return this
    val hls = media.isHls()
    val type = if (hls) MimeTypes.APPLICATION_M3U8 else media.mimeType ?: MimeTypes.VIDEO_MP4
    return DownloadRequest.Builder(id, media.url.toUri()).setMimeType(type).setStreamKeys(streamKeys)
        .setCustomCacheKey(customCacheKey.takeUnless { hls }).setKeySetId(keySetId).setData(data).build()
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal fun isExpiredMediaResponse(failure: Exception): Boolean = generateSequence<Throwable>(failure) { it.cause }
    .filterIsInstance<androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException>()
    .any { it.responseCode in setOf(401, 403, 404, 410) }
private const val MAX_TRANSFER_RESTARTS = 3

internal fun isTransientTransferHttpStatus(status: Int?): Boolean = status == null || status == 408 || status == 429 || status in 500..599

internal fun parseMediaVerification(id: String, value: String): MediaVerification? = runCatching {
    Gson().fromJson(value, MediaVerification::class.java)?.takeIf { event ->
        event.id == id && event.url.toHttpUrlOrNull() != null && event.browserUrl.toHttpUrlOrNull() != null
    }
}.getOrNull()

internal fun mediaVerificationEvents(context: Context): Map<String, MediaVerification> =
    context.getSharedPreferences("media-verification", Context.MODE_PRIVATE).all.mapNotNull { (id, value) ->
        parseMediaVerification(id, value.toString())?.let { id to it }
    }.toMap()

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
        if (!wakeLock.isHeld) wakeLock.acquire(6 * 60 * 60_000L)
    }

    override fun onDestroy() {
        if (wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
    }

    override fun getDownloadManager(): DownloadManager = OfflineDownloads.manager(this)
    override fun getScheduler(): Scheduler = androidx.media3.exoplayer.scheduler.PlatformScheduler(this, 10)
    override fun onTimeout(startId: Int, fgsType: Int) {
        // Android 15+ limits data-sync foreground-service runtime.
        DownloadStore.operation("transfer", "service-timeout", "Android stopped background downloads at its time limit. Reopen the app and resume paused downloads.")
        OfflineDownloads.manager(this).pauseDownloads()
        stopSelf()
    }
    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification {
        val active = downloads.filter { it.state == Download.STATE_DOWNLOADING }
        val records = Exports.all(this)
        val known = active.map { download ->
            val id = download.request.id
            val hls = runCatching { records[id]?.media?.isHls() }.getOrNull() ?: isHlsMimeType(download.request.mimeType)
            val reported = download.percentDownloaded.takeUnless { hls || OfflineDownloads.isGettingLink(id) } ?: -1f
            val coverage = if (hls && !OfflineDownloads.isGettingLink(id)) OfflineDownloads.hlsSample(id)?.percent else null
            mediaCoverageProgress(reported, coverage)
        }
            .filter { it >= 0f }
        val percent = if (active.isNotEmpty() && known.size == active.size) known.average().toInt().coerceIn(0, 100) else null
        val rate = if (active.isEmpty()) { DownloadSpeed.reset(); 0L } else DownloadSpeed.sample()
        val title = if (active.size == 1) String(active[0].request.data).ifBlank { "Downloading episode" }
            else if (active.size > 1) "Downloading ${active.size} episodes" else "Downloads"
        val text = when {
            notMetRequirements != 0 -> "Waiting for internet"
            active.isEmpty() -> "Waiting to download"
            active.all { OfflineDownloads.isGettingLink(it.request.id) } -> "Getting link"
            else -> listOfNotNull(percent?.let { if (active.size > 1) "Average $it%" else "$it%" }, rate.takeIf { it > 0 }?.let { "${Formatter.formatShortFileSize(this, it)}/s" }).joinToString(" · ").ifEmpty { "Downloading" }
        }
        return NotificationCompat.Builder(this, "downloads")
            .setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle(title).setContentText(text)
            .setContentIntent(downloadsPendingIntent(this)).setOnlyAlertOnce(true).setOngoing(true)
            .addAction(0, if (OfflineDownloads.manager(this).downloadsPaused) "Resume" else "Pause",
                android.app.PendingIntent.getForegroundService(this, 31,
                    if (OfflineDownloads.manager(this).downloadsPaused) DownloadService.buildResumeDownloadsIntent(this, OfflineDownloadService::class.java, true)
                    else DownloadService.buildPauseDownloadsIntent(this, OfflineDownloadService::class.java, true),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
            .setProgress(100, percent ?: 0, percent == null).build()
    }

}

/** A persisted job survives process death between a transient failure and its delayed retry. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class DownloadRetryService : android.app.job.JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var retry: Job? = null

    override fun onStartJob(params: android.app.job.JobParameters): Boolean {
        retry = scope.launch {
            try {
                DownloadStore.awaitReady()
                val manager = OfflineDownloads.manager(this@DownloadRetryService)
                val initialized = withTimeoutOrNull(5_000L) {
                    while (!manager.isInitialized) delay(50)
                    true
                } == true
                if (!initialized) {
                    jobFinished(params, true)
                    return@launch
                }
                OfflineDownloads.retryPending(this@DownloadRetryService, manager)
                jobFinished(params, false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                DebugTrace.event("download_retry_deferred", "type" to failure.javaClass.simpleName)
                jobFinished(params, true)
            }
        }
        return true
    }
    override fun onStopJob(params: android.app.job.JobParameters): Boolean {
        retry?.cancel()
        return true
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        fun schedule(context: Context) {
            val job = android.app.job.JobInfo.Builder(11, android.content.ComponentName(context, DownloadRetryService::class.java))
                .setRequiredNetworkType(android.app.job.JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(30_000).setPersisted(true)
                .setBackoffCriteria(30_000, android.app.job.JobInfo.BACKOFF_POLICY_EXPONENTIAL).build()
            runCatching {
                val scheduler = context.getSystemService(android.app.job.JobScheduler::class.java)
                if (scheduler.getPendingJob(job.id) == null) scheduler.schedule(job)
            }.onFailure { DebugTrace.event("download_retry_schedule_failed", "type" to it.javaClass.simpleName) }
        }
    }
}
