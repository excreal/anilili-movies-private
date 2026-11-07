package com.example.animepahedl

internal data class DownloadGroupProgress(val label: String, val percent: Float? = null,
    val running: Boolean = false, val failed: Boolean = false)

/** Saved episodes do not dilute the progress of the episodes currently being processed. */
internal fun downloadGroupProgress(rows: List<DownloadRow>, preparingLinks: Boolean = false): DownloadGroupProgress {
    fun progress(items: List<DownloadRow>, label: String, useBytes: Boolean = false): DownloadGroupProgress {
        val percent = when {
            useBytes && items.all { it.totalBytes > 0 } ->
                (items.sumOf { it.bytes.toDouble() } * 100 / items.sumOf { it.totalBytes.toDouble() }).toFloat()
            items.all { it.percent.isFinite() && it.percent >= 0 } -> items.map { it.percent }.average().toFloat()
            else -> null
        }?.coerceIn(0f, 100f)
        return DownloadGroupProgress(label, percent, running = true)
    }
    val working = rows.filter { !it.complete && !it.failed && !it.deleting }
    val saving = working.filter { it.save in setOf(SaveState.PREPARING, SaveState.CHECKING, SaveState.COPYING) }
    if (saving.isNotEmpty()) {
        val phase = when {
            saving.any { it.save == SaveState.COPYING } -> SaveState.COPYING
            saving.any { it.save == SaveState.CHECKING } -> SaveState.CHECKING
            else -> SaveState.PREPARING
        }
        return progress(saving.filter { it.save == phase }, when (phase) {
            SaveState.COPYING -> "Saving"
            SaveState.CHECKING -> "Checking video"
            else -> "Preparing video"
        })
    }
    val downloading = working.filter { it.transfer == TransferState.DOWNLOADING }
    if (downloading.isNotEmpty()) return progress(downloading, "Downloading", useBytes = true)
    if (preparingLinks) return DownloadGroupProgress("Preparing links", running = true)
    return when {
        working.any { it.transfer == TransferState.WAITING_NETWORK } -> DownloadGroupProgress("Waiting for network")
        working.any { it.save == SaveState.WAITING } -> DownloadGroupProgress("Waiting to save")
        working.any { it.transfer == TransferState.QUEUED } -> DownloadGroupProgress("Queued")
        rows.any { it.failed } -> DownloadGroupProgress("${rows.count { it.failed }} failed", failed = true)
        working.any { it.transfer == TransferState.PAUSED } -> DownloadGroupProgress("Paused")
        rows.any { it.deleting } -> DownloadGroupProgress("Removing")
        rows.isNotEmpty() && rows.all { it.complete } -> DownloadGroupProgress("Saved")
        else -> DownloadGroupProgress("Waiting")
    }
}
