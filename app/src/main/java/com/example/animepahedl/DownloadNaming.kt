package com.example.animepahedl

internal data class DownloadLocation(val showFolder: String, val fileName: String)

internal fun safeFileName(value: String): String = value
    .replace(Regex("[\\p{Cntrl}\\\\/:*?\"<>|]"), "_").trim().trimEnd('.').take(120).ifBlank { "Anime" }

internal fun downloadLocation(show: String, episode: String?, quality: String?, extension: String): DownloadLocation {
    val name = safeFileName(show)
    val number = episode?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
        val parts = value.split('.', limit = 2)
        if (parts.first().all(Char::isDigit)) parts.first().padStart(2, '0') + parts.getOrNull(1)?.let { ".$it" }.orEmpty()
        else safeFileName(value)
    }
    val suffix = listOfNotNull(number?.let { "ep$it" }, quality?.takeIf { it.isNotBlank() }?.let(::safeFileName)).joinToString(" ")
    return DownloadLocation(name, name + if (suffix.isEmpty()) ".$extension" else " $suffix.$extension")
}
