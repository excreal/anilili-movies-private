package com.example.animepahedl

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import java.io.IOException
import java.util.Locale

data class EpisodeSource(val url: String, val quality: String, val audio: String, val preferred: Boolean = false)
data class ResolvedMedia(val url: String, val quality: String, val audio: String,
    val headers: Map<String, String> = emptyMap(), val backupUrls: List<String> = emptyList(),
    val mimeType: String? = null) {
    // Resolvers may return signed URLs and cookies; never include either in diagnostics.
    override fun toString() = "ResolvedMedia(quality=$quality, audio=$audio, mimeType=$mimeType)"
}

internal fun parseEpisodeSources(html: String): List<EpisodeSource> = Jsoup.parse(html).select("button[data-src]").mapNotNull { button ->
    val url = runCatching { button.attr("data-src").toHttpUrl() }.getOrNull() ?: return@mapNotNull null
    val resolution = button.attr("data-resolution").removeSuffix("p").toIntOrNull() ?: return@mapNotNull null
    if (!url.isHttps || resolution <= 0 || button.attr("data-av1") == "1" ||
        !(url.host.contains("kwik") || listOf(".m3u8", ".mp4", ".webm").any { url.encodedPath.endsWith(it, true) })) return@mapNotNull null
    EpisodeSource(url.toString(), "${resolution}p", button.attr("data-audio").trim().lowercase(Locale.ROOT).ifBlank { "und" }, button.hasClass("active"))
}.distinctBy { Triple(it.url, it.quality, it.audio) }

internal fun commonSourceOptions(episodes: List<List<EpisodeSource>>): List<EpisodeSource> {
    if (episodes.isEmpty()) return emptyList()
    val common = episodes.map { list -> list.map { it.quality to it.audio }.toSet() }.reduce { a, b -> a intersect b }
    return episodes.first().filter { it.quality to it.audio in common }.distinctBy { it.quality to it.audio }
}

internal fun selectEpisodeSource(sources: List<EpisodeSource>, quality: String, audio: String): EpisodeSource {
    val language = when (audio) {
        "Original" -> sources.firstOrNull { it.preferred }?.audio ?: sources.firstOrNull()?.audio
        "English" -> "eng"
        "Japanese" -> "jpn"
        else -> audio
    }
    return sources.filter { it.audio == language && (quality == "Best" || it.quality == quality) }
        .maxByOrNull { it.quality.removeSuffix("p").toInt() }
        ?: throw IOException("$quality / ${audioLabel(audio)} is not available for this episode.")
}

private val languageNames by lazy {
    Locale.getISOLanguages().flatMap { code ->
        val locale = Locale.forLanguageTag(code)
        val name = locale.getDisplayLanguage(Locale.ENGLISH)
        listOf(code to name, runCatching { locale.isO3Language }.getOrDefault(code) to name)
    }.toMap()
}
internal fun audioTag(code: String): String = code.uppercase(Locale.ROOT)
internal fun audioLabel(code: String): String = when (code) {
    "und" -> "Unspecified · UND"
    "Original" -> "Original"
    else -> "${languageNames[code.lowercase(Locale.ROOT)] ?: code.uppercase(Locale.ROOT)} · ${audioTag(code)}"
}
