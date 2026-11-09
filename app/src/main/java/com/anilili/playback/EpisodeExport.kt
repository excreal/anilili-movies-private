package com.anilili.playback

import kotlinx.serialization.Serializable

@Serializable
data class ExportedSubtitle(
    val uri: String,
    val label: String,
    val language: String,
    val mimeType: String? = null,
)

/**
 * An episode that now lives as an MP4 in the device's Downloads folder.
 *
 * This is the app's own record, not Media3's. Once an episode is exported the segment cache is
 * usually dropped, so the Media3 download index no longer knows about it — this record is what
 * keeps it in the offline library, and its [uri] is what the player opens.
 */
