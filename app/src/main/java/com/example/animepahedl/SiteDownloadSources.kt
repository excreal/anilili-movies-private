package com.example.animepahedl

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import java.io.IOException
import java.io.StringReader
import java.util.Base64

// Keep legacy short-drama names so older saved-show JSON remains readable; this edition rejects
// those providers before any request and no longer bundles their parsers or metadata clients.
internal enum class SiteDownloadProvider { DRAMATIP, KISS18, NARTO, EROTICMV, KISSKH, DIRECT }
internal val SiteDownloadProvider.contentLabel get() = when (this) {
    SiteDownloadProvider.EROTICMV -> "Movie"
    SiteDownloadProvider.KISSKH -> "K-drama"
    else -> "Video"
}
internal data class SiteDownloadEpisode(val id: String, val number: Int, val title: String, val url: String, val numberLabel: String? = null)
internal val SiteDownloadEpisode.displayNumber get() = numberLabel ?: number.toString()
internal data class SiteShowImage(val url: String, val role: String, val width: Int? = null, val height: Int? = null, val caption: String? = null)
internal data class SiteShowTerm(val name: String, val id: String? = null, val url: String? = null)
internal data class SiteShowRating(val value: Double? = null, val scale: Double? = null, val votes: Long? = null, val source: String? = null, val label: String? = null)
internal data class SiteShowPerson(val name: String, val role: String? = null, val url: String? = null)
internal data class SiteEpisodeMetadata(val number: String, val title: String, val description: String? = null,
    val durationSeconds: Long? = null, val releaseDate: String? = null, val subtitles: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap())
internal data class SiteShowMetadata(val description: String? = null, val alternativeTitles: List<String> = emptyList(),
    val images: List<SiteShowImage> = emptyList(), val genres: List<String> = emptyList(), val categories: List<String> = emptyList(),
    val tags: List<String> = emptyList(), val genreTerms: List<SiteShowTerm> = emptyList(),
    val categoryTerms: List<SiteShowTerm> = emptyList(), val tagTerms: List<SiteShowTerm> = emptyList(),
    val ratings: List<SiteShowRating> = emptyList(), val people: List<SiteShowPerson> = emptyList(),
    val countries: List<String> = emptyList(), val languages: List<String> = emptyList(), val studios: List<String> = emptyList(),
    val status: String? = null, val format: String? = null, val year: Int? = null, val episodeCount: Int? = null,
    val durationSeconds: Long? = null, val releaseDate: String? = null, val updatedAt: String? = null,
    val externalIds: Map<String, String> = emptyMap(), val attributes: Map<String, String> = emptyMap(),
    val episodes: List<SiteEpisodeMetadata> = emptyList())
internal data class SiteDownloadSeries(val provider: SiteDownloadProvider, val id: String, val title: String,
    val poster: String, val url: String, val episodes: List<SiteDownloadEpisode>, val qualities: List<String> = listOf("Best"),
    val description: String = "", val metadata: SiteShowMetadata? = null, val posterThumbnailOverride: String? = null,
    val contentAvailable: Boolean = true)
internal fun SiteDownloadSeries.overviewText(): String = metadata?.description?.takeIf(String::isNotBlank) ?: description
internal fun SiteDownloadSeries.posterThumbnail(): String {
    val raw = posterThumbnailOverride?.takeIf(String::isNotBlank) ?: if (provider == SiteDownloadProvider.EROTICMV) {
        metadata?.images?.firstOrNull { it.role in setOf("video-poster", "poster") }?.url?.takeIf(String::isNotBlank) ?: poster
    } else poster
    return if (provider == SiteDownloadProvider.KISSKH) kisskhArtworkUrl(raw) else raw
}

/** A stale provider URL may be refreshed once for each selected episode, never in a retry loop. */
internal class MediaLinkRefreshGuard {
    private val refreshed = mutableSetOf<String>()
    fun shouldRefresh(provider: SiteDownloadProvider, episodeId: String): Boolean =
        provider != SiteDownloadProvider.DIRECT && refreshed.add("${provider.name}:$episodeId")
}
internal fun SiteDownloadSeries.matchesInspection(provider: SiteDownloadProvider, id: String, expectedGeneration: Int, generation: Int): Boolean =
    expectedGeneration == generation && this.provider == provider && this.id == id
internal fun mergeInspectionMetadata(current: SiteDownloadSeries, expectedProvider: SiteDownloadProvider, expectedId: String,
    expectedGeneration: Int, generation: Int, metadata: SiteShowMetadata): SiteDownloadSeries =
    if (current.matchesInspection(expectedProvider, expectedId, expectedGeneration, generation)) current.copy(metadata = metadata) else current
internal val SiteDownloadSeries.isSingleVideo get() = provider in setOf(SiteDownloadProvider.DIRECT, SiteDownloadProvider.EROTICMV)
internal data class MediaVariant(val url: String, val quality: String = "Original", val backupUrls: List<String> = emptyList(),
    val mimeType: String = "video/mp4", val headers: Map<String, String> = emptyMap(), val width: Int = 0, val height: Int = 0, val subtitles: List<MediaSubtitle> = emptyList())

internal fun hostMatchesDomain(host: String, domain: String) = host == domain || host.endsWith(".$domain")
internal fun siteDownloadInput(raw: String): HttpUrl {
    val input = decodeEroticMedia(raw.trim())
    val url = runCatching { input.toHttpUrl() }.getOrElse { throw IOException("Enter a supported HTTP(S) video or series link.") }
    require(url.username.isEmpty() && url.password.isEmpty()) { "Links must not contain credentials." }
    return url
}
internal fun siteDownloadProvider(url: HttpUrl): SiteDownloadProvider = when {
    hostMatchesDomain(url.host, "tiktok.com") -> throw IOException("TikTok is not supported by this downloader.")
    hostMatchesDomain(url.host, "dramatip.net") || url.host in setOf("kiss18.vip", "www.kiss18.vip", "narto-drama.com", "www.narto-drama.com", "edge.narto-drama.com") ->
        throw IOException("Short drama sources aren't included in this edition.")
    hostMatchesDomain(url.host, "eroticmv.com") -> SiteDownloadProvider.EROTICMV
    url.host in setOf("kisskh.do", "www.kisskh.do") -> SiteDownloadProvider.KISSKH
    isHlsMedia(url) || isSupportedVideoFile(url) -> SiteDownloadProvider.DIRECT
    else -> throw IOException("Unsupported link. Use EroticMV, KissKH, or a direct HLS/MP4/WebM URL.")
}
internal fun isHlsMedia(url: HttpUrl) = url.encodedPath.contains(".m3u8", true)
internal fun isSupportedVideoFile(url: HttpUrl) = listOf(".mp4", ".m4v", ".webm").any { url.encodedPath.endsWith(it, true) } ||
    url.queryParameter("mime_type") in setOf("video_mp4", "video/mp4", "video/webm")
internal fun resolveProviderUrl(base: HttpUrl, raw: String): String = base.resolve(raw.trim())?.takeIf { it.username.isEmpty() && it.password.isEmpty() }?.toString()
    ?: throw IOException("The provider returned an invalid media link.")
internal fun parseSiteDownloadSelection(input: String, episodes: List<SiteDownloadEpisode>): List<SiteDownloadEpisode> {
    val selection = input.trim()
    if (selection.isBlank()) return episodes
    if (selection.equals("none", true)) return emptyList()
    require(!selection.trimStart().startsWith(',') && !selection.trimEnd().endsWith(',') && !Regex(",\\s*,").containsMatchIn(selection)) {
        "Episode selections cannot contain empty items."
    }
    if (episodes.any { it.numberLabel != null }) {
        val labels = episodes.associateWith { it.displayNumber.toBigDecimal() }
        val selected = linkedSetOf<SiteDownloadEpisode>()
        selection.replace(Regex("\\s*-\\s*"), "-").split(Regex("[,\\s]+")).forEach { token ->
            val range = token.split('-').map { it.toBigDecimalOrNull() ?: throw IOException("Use episode numbers or ranges, such as 1-5,9.") }
            require(range.size in 1..2 && range.first() <= range.last()) { "Invalid episode selection." }
            require(labels.values.any { it.compareTo(range.first()) == 0 } && labels.values.any { it.compareTo(range.last()) == 0 }) { "This episode is unavailable." }
            selected += episodes.filter { labels.getValue(it) >= range.first() && labels.getValue(it) <= range.last() }
        }
        return episodes.filter { it in selected }
    }
    val available = episodes.map { it.number }.toSet()
    val numbers = mutableSetOf<Int>()
    for (token in selection.replace(Regex("\\s*-\\s*"), "-").split(Regex("[,\\s]+"))) {
        val parts = token.trim().split('-').map { it.trim().toIntOrNull() ?: throw IOException("Use episode numbers or ranges, such as 1-5,9.") }
        require(parts.size in 1..2 && parts.all { it in 1..10000 }) { "Invalid episode selection." }
        val first = parts.first(); val last = parts.last()
        require(first <= last) { "Episode ranges must be ascending." }
        for (number in first..last) {
            require(number in available) { "Episode $number is unavailable." }
            numbers += number
        }
    }
    return episodes.filter { it.number in numbers }
}

internal fun parseProviderTitleAndPoster(html: String, url: HttpUrl): Pair<String, String> {
    val doc = Jsoup.parse(html)
    val title = doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty().ifBlank { doc.title().trim() }
    val image = doc.selectFirst("meta[property=og:image]")?.attr("content").orEmpty()
    return title to if (image.isBlank()) "" else runCatching { resolveProviderUrl(url, image) }.getOrDefault("")
}
internal fun cleanProviderTitle(raw: String, provider: SiteDownloadProvider): String = when (provider) {
    SiteDownloadProvider.EROTICMV -> raw.removePrefix("Watch ").substringBeforeLast(" - ", raw.removePrefix("Watch "))
    else -> raw
}.trim()
internal fun jsonAfterMarker(text: String, marker: Regex): JsonElement? {
    val match = marker.find(text) ?: return null
    return runCatching { JsonParser.parseReader(JsonReader(StringReader(text.substring(match.range.last + 1)))) }.getOrNull()
}
internal fun JsonObject.jsonString(vararg names: String): String = names.firstNotNullOfOrNull { name ->
    get(name)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf(String::isNotBlank)
}.orEmpty()
internal fun JsonObject.jsonInt(vararg names: String): Int = jsonString(*names).toIntOrNull() ?: 0
internal fun JsonObject.jsonObjectOrEmpty(name: String): JsonObject = get(name)?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject()
internal fun JsonObject.jsonArrayOrEmpty(name: String): List<JsonElement> = get(name)?.takeIf { it.isJsonArray }?.asJsonArray?.toList().orEmpty()
internal fun decodeEroticMedia(raw: String): String {
    val encoded = Regex("(?i)^https?://([A-Za-z0-9+/_=-]+)\\.m3u8/?$").matchEntire(raw.trim())?.groupValues?.get(1) ?: return raw.trim()
    for (decoder in listOf(Base64.getDecoder(), Base64.getUrlDecoder())) {
        val decoded = runCatching { decoder.decode(encoded).toString(Charsets.UTF_8).trim().toHttpUrl() }.getOrNull()
        if (decoded != null && isHlsMedia(decoded)) return decoded.toString()
    }
    return raw.trim()
}
internal fun eroticVideoSource(page: String): String? {
    val doc = Jsoup.parse(page)
    for ((selector, attr) in listOf("meta[property=og:video:url]" to "content", "video source[src]" to "src", "video[src]" to "src")) {
        doc.selectFirst(selector)?.attr(attr)?.takeIf(String::isNotBlank)?.let { return it }
    }
    return jsonAfterMarker(page, Regex("\"single_video_url\"\\s*:"))?.takeIf { it.isJsonPrimitive }?.asString
}
internal fun chooseMediaVariant(variants: List<MediaVariant>, quality: String): MediaVariant {
    require(variants.isNotEmpty()) { "No supported media variants found." }
    val target = quality.removeSuffix("p").toIntOrNull()
    if (quality !in setOf("Best", "Original") && target == null) throw IOException("Invalid quality selection.")
    val candidates = if (target == null) variants else variants.filter { mediaHeight(it) <= target }
    if (candidates.isEmpty()) return variants.minByOrNull { mediaHeight(it) }!!
    return candidates.maxWithOrNull(compareBy<MediaVariant> { mediaHeight(it) }.thenBy { if (it.mimeType == "application/x-mpegURL") 1 else 0 })!!
}
private fun mediaHeight(variant: MediaVariant): Int = variant.quality.removeSuffix("p").toIntOrNull() ?: minOf(variant.width, variant.height).coerceAtLeast(0)

