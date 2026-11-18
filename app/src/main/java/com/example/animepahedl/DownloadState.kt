package com.example.animepahedl

enum class TransferState { QUEUED, RESOLVING, DOWNLOADING, PAUSED, WAITING_NETWORK, COMPLETE, FAILED, REMOVING }
enum class SaveState { NONE, WAITING, PREPARING, CHECKING, COPYING, FAILED }
enum class Availability { STAGING, SAVED }
enum class DownloadAction { PAUSE, RESUME, RETRY, RETRY_SAVE, VERIFY, DELETE }
enum class FailureReason { NETWORK, VERIFICATION, EXPIRED_SOURCE, STORAGE_FULL, FOLDER_ACCESS, INCOMPATIBLE_EXPORT, DELETION, UNKNOWN }

internal fun normalizedProgress(percent: Float): Float = if (percent.isFinite() && percent >= 0) percent.coerceAtMost(100f) else -1f

internal fun byteProgress(bytes: Long, total: Long): Float =
    if (bytes >= 0 && total > 0) normalizedProgress((bytes.toDouble() * 100 / total).toFloat()) else -1f

// An HLS transfer contains segment framing and may include overlapping or clipped byte ranges.
internal fun displayTransferTotal(bytes: Long, reportedTotal: Long, hls: Boolean): Long =
    if (!hls && reportedTotal > 0 && bytes in 0..reportedTotal) reportedTotal else -1L

internal fun mediaCoverageProgress(reported: Float, coverage: Float?): Float =
    normalizedProgress(coverage ?: reported).let { if (it >= 0) it.coerceAtMost(99f) else it }

data class DownloadRow(val id: String, val title: String, val transfer: TransferState, val percent: Float,
    val bytes: Long = 0, val totalBytes: Long = -1, val savedUri: String = "",
    val thumbnail: String = "", val audioCode: String? = null, val quality: String? = null,
    val save: SaveState = SaveState.NONE, val availability: Availability = Availability.STAGING,
    val failure: FailureReason? = null, val explanation: String? = null, val deleting: Boolean = false, val showId: String = "", val episodeId: String = "", val showTitle: String = "", val episodeNumber: String = "", val retryable: Boolean = false, val transferControllable: Boolean = false,
    val estimatedTotalBytes: Long = -1L, val requestedQuality: String? = null) {
    val complete get() = availability == Availability.SAVED
    val saving get() = save in setOf(SaveState.WAITING, SaveState.PREPARING, SaveState.CHECKING, SaveState.COPYING)
    val failed get() = transfer == TransferState.FAILED || save == SaveState.FAILED || failure != null
    val actions: Set<DownloadAction> get() = buildSet {
        add(DownloadAction.DELETE)
        if (deleting) return@buildSet
        if (failure == FailureReason.VERIFICATION) add(DownloadAction.VERIFY)
        if (save == SaveState.FAILED) add(DownloadAction.RETRY_SAVE)
        else if (transfer == TransferState.FAILED && !complete && retryable) add(DownloadAction.RETRY)
        if (transferControllable && !id.startsWith("direct:") && !complete && save == SaveState.NONE && failure == null) {
            if (transfer == TransferState.PAUSED) add(DownloadAction.RESUME)
            if (transfer in setOf(TransferState.QUEUED, TransferState.RESOLVING, TransferState.DOWNLOADING, TransferState.WAITING_NETWORK)) add(DownloadAction.PAUSE)
        }
    }
    val status get() = explanation ?: when {
        complete -> "Ready"
        failure == FailureReason.VERIFICATION -> "Verify session"
        failure == FailureReason.FOLDER_ACCESS -> "Choose folder"
        failure == FailureReason.STORAGE_FULL -> "Storage full"
        failure == FailureReason.EXPIRED_SOURCE -> "Link expired"
        save == SaveState.FAILED -> "Couldn’t save"
        save == SaveState.WAITING -> "Waiting to save"
        save == SaveState.PREPARING -> "Processing"
        save == SaveState.CHECKING -> "Checking"
        save == SaveState.COPYING -> "Saving"
        else -> when (transfer) {
            TransferState.QUEUED -> "Queued"
            TransferState.RESOLVING -> "Getting link"
            TransferState.DOWNLOADING -> "Downloading"
            TransferState.PAUSED -> "Paused"
            TransferState.WAITING_NETWORK -> "Waiting for internet"
            TransferState.COMPLETE -> "Ready offline"
            TransferState.FAILED -> "Download failed"
            TransferState.REMOVING -> "Removing"
        }
    }
}

internal data class DownloadIdentity(val anime: Anime, val episode: Episode, val quality: String, val audio: String,
    val source: String? = null, val locator: String? = null) {
    val isSiteDownload get() = source == "sites"
    val isSingleVideo get() = isSiteDownload && anime.type == "Video"
    // Absent fields in old Gson records stay null. Never change an AnimePahe download key.
    val key get() = if (!isSiteDownload) "${anime.session}-${episode.session}-$quality-$audio" else {
        val value = listOf(source, anime.session, episode.session, quality, audio).joinToString("\u0000")
        "sites:" + java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
    companion object {
        fun siteDownload(series: SiteDownloadSeries, episode: SiteDownloadEpisode, quality: String, audio: String): DownloadIdentity {
            val poster = series.posterThumbnail()
            return DownloadIdentity(Anime("sites:${series.provider.name}:${series.id}", series.title, poster,
                "", if (series.isSingleVideo) "Video" else "Series", ""), Episode(episode.id, episode.displayNumber, poster, ""),
                quality, audio, "sites", episode.url)
        }
    }
}

internal data class DownloadRecoveryEligibility(val retryable: Boolean, val transferControllable: Boolean)

internal fun matchesDownloadQuality(requested: String, existingRequest: String?, resolved: String?): Boolean = when (requested) {
    "Best", "Original" -> existingRequest in setOf("Best", "Original")
    else -> requested == existingRequest || requested == resolved
}

internal fun matchesQueuedVideo(identity: DownloadIdentity, existing: ExportRecord, id: String): Boolean {
    if (id == identity.key) return true
    val other = existing.identity ?: return false
    return other.isSiteDownload && other.anime.session == identity.anime.session && other.episode.session == identity.episode.session &&
        matchesDownloadQuality(identity.quality, other.quality, existing.quality)
}

/** Recovery actions must match the source context required by their implementation. */
internal fun downloadRecoveryEligibility(record: ExportRecord?): DownloadRecoveryEligibility {
    val identity = record?.identity?.takeIf { it.isSiteDownload } ?: return DownloadRecoveryEligibility(false, false)
    val canResolve = !identity.locator.isNullOrBlank()
    return DownloadRecoveryEligibility(retryable = canResolve, transferControllable = canResolve || record.media != null)
}

internal fun classifyFailure(message: String): FailureReason = when {
    message.contains("space", true) || message.contains("ENOSPC", true) -> FailureReason.STORAGE_FULL
    message.contains("permission", true) || message.contains("folder", true) || message.contains("SecurityException") -> FailureReason.FOLDER_ACCESS
    message.contains("FFmpeg", true) || message.contains("track", true) || message.contains("duration", true) -> FailureReason.INCOMPATIBLE_EXPORT
    message.contains("403") || message.contains("410") -> FailureReason.EXPIRED_SOURCE
    message.contains("network", true) || message.contains("timeout", true) -> FailureReason.NETWORK
    else -> FailureReason.UNKNOWN
}

internal data class PreparationRecord(val anime: Anime, val episodes: List<Episode>, val quality: String, val audio: String)
