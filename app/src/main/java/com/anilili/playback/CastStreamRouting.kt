package com.anilili.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes

/**
 * Rewrites the queue that is handed to a Cast receiver so it can fetch what this device can.
 *
 * A receiver makes its own requests and takes none of our headers, so two things this app relies
 * on silently stop being true the moment playback moves off the phone: a `Referer` no longer
 * reaches a gated CDN, and a subtitle host that omits CORS is refused by the receiver's own
 * fetch. [StreamCorsProxy] answers both by carrying the referer inside the URL and replying with
 * `Access-Control-Allow-Origin: *`, so the rewrite is only ever a change of address.
 *
 * Deliberately narrow about what it rewrites:
 *
 * - **The stream, only when the provider named a referer.** That is this app's own signal that the
 *   source is gated; a source that never asked for one plays on a receiver today, and routing it
 *   through third-party infrastructure could only make it slower or more fragile.
 * - **WebVTT subtitles, always.** CORS is a separate gate from the referer, and a track dropped by
 *   the receiver looks exactly like a source with no subtitles. TTML is left alone because the
 *   proxy answers `text/vtt` and re-labelling a track is not a translation.
 *
 * **Nothing is published unverified.** [StreamCorsProxy] is third-party infrastructure that its own
 * documentation says must be survivable, and it refuses some pairs outright — measured 2026-09-20,
 * it answers 500 with an empty body for a `megaplay.buzz` referer carrying a target on another host,
 * which is exactly the pair anikoto produces (`fetch.nexabloom.top`). A receiver handed that fetches
 * the 500 and reports nothing but `STATE_IDLE`, so the caller preflights the stream and each
 * sidecar URL with the receiver's Origin before publishing them.
 *
 * Since some point after 2026-09-20 it also allowlists the request's `Origin` to miruro's own
 * sites, which a receiver page can never send — so for casting it may refuse a particular route,
 * and the caller's question has to be asked with a receiver's Origin to hear that (see
 * `PlaybackService.receiverCanFetch`).
 *
 * The original items are kept by the caller and restored on the way back, so a local player never
 * inherits the detour. [restoreLocalItem] is the fallback for when they cannot be: `mediaId` is
 * the stream URL the player screen prepared, which is enough to undo the address change.
 */
internal object CastStreamRouting {

    internal data class ProxyProbeUrls(val stream: String?, val subtitles: List<String>) {
        fun all(): List<String> = listOfNotNull(stream) + subtitles
    }

    /**
     * Whether a receiver could play this queue at all, before any question of what it can reach.
     *
     * A `playlistKey` is the one disqualifier, and it is absolute: those playlists arrive XOR'd and
     * are decoded by [FlixcloudPlaylistDataSource] and [UniqueStreamKeyDataSource], which are
     * `DataSource` wrappers on *this* device. A receiver fetching the same address gets the
     * encoded bytes and no way to read them, so ReAnime and UniqueStream cannot be cast however
     * healthy the network is. `EpisodeDownloads.canDownload` gates the downloader the same way, for
     * the same reason — it just gained the decoders and this cannot.
     *
     * An empty queue means the episode is playing in a WebView rather than here (an embed source),
     * which has no addresses to hand over either.
     *
     * Asked so a viewer is told, rather than left waiting out the load watchdog for something that
     * was never going to start; see `PlaybackService.handOverToReceiver`.
     */
    fun canCastQueue(items: List<MediaItem>, playlistKey: String?): Boolean =
        items.isNotEmpty() && playlistKey == null

    /**
     * The gated stream and each WebVTT sidecar that will use the proxy. Testing them individually
     * matters because a proxy that accepts the stream can refuse a subtitle on another host, and
     * one refused receiver track can prevent the whole load. This app hands a receiver one episode.
     */
    fun proxyProbeUrls(items: List<MediaItem>, referer: String?): ProxyProbeUrls {
        val item = items.firstOrNull() ?: return ProxyProbeUrls(null, emptyList())
        val config = item.localConfiguration ?: return ProxyProbeUrls(null, emptyList())
        val source = config.uri.toString()
        val seed = item.mediaId.takeIf { it.isNotBlank() } ?: source
        val stream = referer?.takeIf(String::isNotBlank)
            ?.let { StreamCorsProxy.playlist(source, it, seed) }
        val subtitles = config.subtitleConfigurations
            .filter { it.mimeType?.equals(MimeTypes.TEXT_VTT, ignoreCase = true) == true }
            .mapNotNull { StreamCorsProxy.subtitle(it.uri.toString(), referer, seed) }
        return ProxyProbeUrls(stream, subtitles)
    }

    /**
     * The queue to hand a receiver, or [items] unchanged when nothing in it needs the proxy.
     *
     * [proxyServingUrls] contains the addresses the preflight confirmed. A refused stream remains
     * direct, as before; a refused WebVTT sidecar is omitted so it cannot fail the receiver load.
     */
    fun castItems(items: List<MediaItem>, referer: String?, proxyServingUrls: Set<String>): List<MediaItem> =
        items.map { castItem(it, referer, proxyServingUrls) }

    /** The queue to hand back to the local player, undoing [castItems] by URL. */
    fun localItems(items: List<MediaItem>): List<MediaItem> = items.map(::restoreLocalItem)

    private fun castItem(item: MediaItem, referer: String?, proxyServingUrls: Set<String>): MediaItem {
        val source = item.localConfiguration?.uri?.toString() ?: return item
        val seed = item.mediaId.takeIf { it.isNotBlank() } ?: source
        val original = item.localConfiguration?.subtitleConfigurations.orEmpty()
        // A gated stream is the only one worth the detour; see the class comment.
        val streamUrl = referer?.takeIf(String::isNotBlank)
            ?.let { StreamCorsProxy.playlist(source, it, seed) }
            ?.takeIf { it in proxyServingUrls }
        val subtitles = original.mapNotNull { subtitle ->
            val isVtt = subtitle.mimeType?.equals(MimeTypes.TEXT_VTT, ignoreCase = true) == true
            if (!isVtt) return@mapNotNull subtitle
            // A WebVTT track is on the cast queue *because* the proxy lends it CORS. Without that
            // there is no knowing whether its host answers a receiver, and Google's receiver fails
            // the whole load over one track it cannot fetch — so the track goes rather than the
            // episode. In-manifest renditions are untouched, and the picture is what was asked for.
            val proxied = StreamCorsProxy.subtitle(subtitle.uri.toString(), referer, seed)
                ?: return@mapNotNull subtitle
            if (proxied !in proxyServingUrls) return@mapNotNull null
            subtitle.buildUpon().setUri(Uri.parse(proxied)).build()
        }
        if (streamUrl == null && subtitles == original) return item
        return item.buildUpon()
            .apply { streamUrl?.let { setUri(it) } }
            .setSubtitleConfigurations(subtitles)
            .build()
    }

    /**
     * One item with its proxied stream address swapped back for the one it was prepared from.
     *
     * Subtitles are left as they are: a proxied WebVTT URL is a working WebVTT URL locally too,
     * and this path only runs when the snapshot taken on the way out has been lost.
     */
    fun restoreLocalItem(item: MediaItem): MediaItem {
        val current = item.localConfiguration?.uri?.toString() ?: return item
        if (!StreamCorsProxy.isProxied(current)) return item
        val original = item.mediaId.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?: return item
        return item.buildUpon().setUri(original).build()
    }
}
