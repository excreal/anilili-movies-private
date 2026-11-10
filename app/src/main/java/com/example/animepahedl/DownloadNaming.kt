package com.example.animepahedl

internal data class DownloadLocation(val showFolder: String, val fileName: String)

internal fun safeFileName(value: String): String = value
    .replace(Regex("[\\p{Cntrl}\\\\/:*?\"<>|]"), "_").trim().trimEnd('.').take(120).ifBlank { "Anime" }

internal fun subtitleSidecarName(mediaFileName: String, language: String, index: Int): String {
    val stem = mediaFileName.substringBeforeLast('.', mediaFileName)
    val tag = language.trim().takeIf { it.matches(Regex("(?i)[a-z]{2,3}(?:[-_][a-z0-9]{2,8})*")) }
        ?.lowercase()?.replace('_', '-') ?: "track-${index + 1}"
    return "$stem.$tag.vtt"
}

internal fun downloadLocation(show: String, episode: String?, quality: String?, extension: String, audio: String? = null): DownloadLocation {
    val name = safeFileName(show)
    val number = episode?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
        val parts = value.split('.', limit = 2)
        if (parts.first().all(Char::isDigit)) parts.first().padStart(2, '0') + parts.getOrNull(1)?.let { ".$it" }.orEmpty()
        else safeFileName(value)
    }
    val suffix = listOfNotNull(number?.let { "ep$it" }, quality?.takeIf { it.isNotBlank() }?.let(::safeFileName), audio?.takeIf { it.isNotBlank() }?.let { safeFileName(audioTag(it)) }).joinToString(" ")
    return DownloadLocation(name, name + if (suffix.isEmpty()) ".$extension" else " $suffix.$extension")
}

/** The source episode travels with the job; queue/completion position never identifies a file. */
internal fun ExportRecord.outputLocation(extension: String): DownloadLocation {
    val source = identity
    val parts = title.split(" · ")
    return downloadLocation(source?.anime?.title ?: showTitle ?: parts.first(),
        if (source?.isSingleVideo == true) null else source?.episode?.number ?: episodeNumber ?: parts.getOrNull(1)?.removePrefix("Episode "),
        quality ?: source?.quality ?: parts.getOrNull(2)?.takeIf { it != "Best" }, extension,
        audio ?: source?.audio ?: parts.getOrNull(3))
}

internal fun ExportRecord.episodeTitle(): String = identity?.let {
    it.anime.title + (if (it.isSingleVideo) "" else " · Episode ${it.episode.number}") + " · ${quality ?: it.quality} · ${audioTag(audio ?: it.audio)}"
} ?: title
