package com.anilili.ui.watch

import com.anilili.data.settings.SettingsStore
import com.anilili.data.settings.WATCHED_THRESHOLD_MAX_PERCENT
import com.anilili.data.settings.WATCHED_THRESHOLD_MIN_PERCENT
import com.anilili.data.model.Category
import com.anilili.data.model.SourceVariant

data class WatchSourceOption(
    val provider: String,
    val category: Category,
    val hasCurrentEpisode: Boolean,
    val episodeCount: Int,
    val variant: SourceVariant = SourceVariant.defaultFor(category),
    /**
     * This pair has already been tried for this episode and did not answer, so it sits at the end
     * of the picker and is drawn faded. Still selectable: see [visibleSourceOptions].
     */
    val recentlyFailed: Boolean = false,
)

/**
 * What a server/audio pair actually turned out to offer, learned when its sources were resolved.
 *
 * The catalog only knows sub vs dub. Which spoken language a dub is in, and which subtitle tracks
 * ride along, are inside the resolved [com.anilili.data.model.SourcesResult] — so they are
 * only knowable after a resolve. The source-validation pass already performs exactly that resolve
 * for every candidate and used to keep nothing but a boolean; this is that discarded detail.
 *
 * [known] separates "we checked and it has no subtitle tracks" (common — plenty of servers ship
 * hardsubbed video) from "we have not checked yet", so the picker never claims a language is
 * missing while it is still looking.
 */

internal fun shouldSyncAniListProgress(
    episodeNumber: Double,
    positionMs: Long,
    durationMs: Long,
    thresholdPercent: Int = SettingsStore.watchedThresholdPercent.value,
): Boolean {
    if (episodeNumber < 1 || episodeNumber % 1.0 != 0.0) return false
    if (durationMs < MIN_ANILIST_SYNC_DURATION_MS || positionMs <= 0) return false
    val fraction = thresholdPercent
        .coerceIn(WATCHED_THRESHOLD_MIN_PERCENT, WATCHED_THRESHOLD_MAX_PERCENT) / 100.0
    return positionMs.toDouble() / durationMs >= fraction
}


private const val MIN_ANILIST_SYNC_DURATION_MS = 60_000L

/**
 * How far into an episode the next one is resolved ahead of time.
 *
 * Late enough that the current episode's own startup is finished and the viewer has clearly
 * settled on this title, early enough to be ready long before the credits.
 */
