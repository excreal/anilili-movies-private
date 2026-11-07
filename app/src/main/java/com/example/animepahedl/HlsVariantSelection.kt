package com.example.animepahedl

/** Keep the selected video and its external renditions together for the cache-only remuxer. */
internal fun selectCachedHlsVariant(playlist: String, quality: String): String {
    val lines = playlist.lines()
    fun attributes(line: String): Map<String, String> =
        Regex("""([A-Z0-9-]+)=("[^"]*"|[^,]*)""").findAll(line.substringAfter(':', ""))
            .associate { it.groupValues[1] to it.groupValues[2].removeSurrounding("\"") }
    data class Variant(val tag: Int, val uri: Int, val attributes: Map<String, String>) {
        val height: Int get() = attributes["RESOLUTION"]?.split('x', 'X')?.mapNotNull(String::toIntOrNull)
            ?.takeIf { it.size == 2 }?.minOrNull() ?: 0
        val bandwidth: Long get() = (attributes["AVERAGE-BANDWIDTH"] ?: attributes["BANDWIDTH"])?.toLongOrNull() ?: 0L
        val hasVideo: Boolean get() = height > 0 || attributes["CODECS"].orEmpty().split(',').any {
            it.trim().startsWith("avc", true) || it.trim().startsWith("hvc", true) || it.trim().startsWith("hev", true) ||
                it.trim().startsWith("av01", true) || it.trim().startsWith("vp", true)
        }
    }
    val variants = lines.indices.filter { lines[it].trim().startsWith("#EXT-X-STREAM-INF:") }.map { index ->
        val uri = (index + 1 until lines.size).firstOrNull {
            val line = lines[it].trim()
            line.isNotEmpty() && (!line.startsWith('#') || line.startsWith("#EXT-X-STREAM-INF:"))
        }
        require(uri != null && !lines[uri].trim().startsWith('#')) { "HLS variant URI is missing." }
        Variant(index, uri, attributes(lines[index].trim()))
    }
    if (variants.isEmpty()) return playlist
    val videos = variants.filter { it.hasVideo }.ifEmpty { variants }
    val target = quality.removeSuffix("p").toIntOrNull()
    val eligible = if (target == null || videos.none { it.height > 0 }) videos else videos.filter { it.height in 1..target }
    val order = compareBy<Variant> { it.height }.thenBy { it.bandwidth }
    val chosen = if (eligible.isEmpty()) videos.minWithOrNull(order)!! else eligible.maxWithOrNull(order)!!
    val audio = lines.indices.filter { index ->
        if (!lines[index].trim().startsWith("#EXT-X-MEDIA:")) false else attributes(lines[index]).let {
            it["TYPE"] == "AUDIO" && it["GROUP-ID"] == chosen.attributes["AUDIO"]
        }
    }.maxByOrNull { index ->
        val info = attributes(lines[index])
        (if (info["DEFAULT"] == "YES") 2 else 0) + (if (info["AUTOSELECT"] == "YES") 1 else 0)
    }
    val excluded = variants.filter { it != chosen }.flatMap { listOf(it.tag, it.uri) }.toSet()
    return lines.filterIndexed { index, raw ->
        val line = raw.trim()
        when {
            index in excluded -> false
            line.startsWith("#EXT-X-I-FRAME-STREAM-INF:") || line.startsWith("#EXT-X-IMAGE-STREAM-INF:") -> false
            line.startsWith("#EXT-X-MEDIA:") -> {
                val rendition = attributes(line)
                val type = rendition["TYPE"]
                type != null && type in setOf("AUDIO", "SUBTITLES", "CLOSED-CAPTIONS") &&
                    rendition["GROUP-ID"] == chosen.attributes[type] && (type != "AUDIO" || index == audio)
            }
            else -> true
        }
    }.joinToString("\n")
}
