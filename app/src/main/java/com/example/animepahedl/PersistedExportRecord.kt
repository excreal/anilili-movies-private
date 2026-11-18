package com.example.animepahedl

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** Gson can bypass Kotlin constructors and leave non-null fields null in older or damaged rows.
 * Reconstruct records at the disk boundary so one bad optional field cannot hide saved media.
 * Invalid rows remain on disk; this only controls the safe in-memory snapshot.
 */
internal fun parsePersistedExportRecord(json: String): ExportRecord? = runCatching {
    val value = JsonParser.parseString(json).objectOrNull() ?: return@runCatching null
    // A random object is not a download. Missing fields in recognizable older records are OK.
    if (listOf("title", "uri", "identity", "media").none(value::has)) return@runCatching null
    val title = value.string("title")?.takeIf(String::isNotBlank) ?: "Downloaded video"
    val identity = value.get("identity")?.objectOrNull()?.identity(title)
    ExportRecord(
        title = title,
        tree = value.string("tree").orEmpty(),
        folder = value.string("folder").orEmpty(),
        uri = value.string("uri").orEmpty(),
        error = value.string("error").orEmpty(),
        bytes = value.get("bytes")?.let { runCatching { it.asLong }.getOrNull() }?.coerceAtLeast(0) ?: 0,
        thumbnail = value.string("thumbnail"),
        audio = value.string("audio"),
        quality = value.string("quality"),
        showTitle = value.string("showTitle"),
        episodeNumber = value.string("episodeNumber"),
        identity = identity,
        pendingUri = value.string("pendingUri"),
        media = value.get("media")?.objectOrNull()?.media(identity)
    )
}.getOrNull()

private fun JsonElement.objectOrNull(): JsonObject? = if (isJsonObject) asJsonObject else null

private fun JsonElement.stringOrNull(): String? =
    takeIf { isJsonPrimitive && asJsonPrimitive.isString }?.asString

private fun JsonObject.string(name: String): String? = get(name)?.stringOrNull()

private fun JsonObject.identity(fallbackTitle: String): DownloadIdentity? {
    val anime = get("anime")?.objectOrNull() ?: return null
    val episode = get("episode")?.objectOrNull() ?: return null
    val animeSession = anime.string("session")?.takeIf(String::isNotBlank) ?: return null
    val episodeSession = episode.string("session")?.takeIf(String::isNotBlank) ?: return null
    // These form the persisted engine key. Do not invent replacement identity components.
    val quality = string("quality") ?: return null
    val audio = string("audio") ?: return null
    return DownloadIdentity(
        Anime(animeSession, anime.string("title") ?: fallbackTitle, anime.string("poster").orEmpty(),
            anime.string("year").orEmpty(), anime.string("type").orEmpty(),
            anime.string("status").orEmpty(), anime.string("synopsis").orEmpty()),
        Episode(episodeSession, episode.string("number").orEmpty(),
            episode.string("snapshot").orEmpty(), episode.string("duration").orEmpty()),
        quality, audio, string("source"), string("locator")
    )
}

private fun JsonObject.media(identity: DownloadIdentity?): ResolvedMedia? {
    val url = string("url")?.takeIf(String::isNotBlank) ?: return null
    val headers = get("headers")?.objectOrNull()?.entrySet()?.mapNotNull { (name, value) ->
        value.stringOrNull()?.let { name to it }
    }?.toMap().orEmpty()
    val backupUrls = get("backupUrls")?.takeIf { it.isJsonArray }?.asJsonArray
        ?.mapNotNull { it.stringOrNull()?.takeIf(String::isNotBlank) }.orEmpty()
    val subtitles = get("subtitles")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { element ->
        val track = element.objectOrNull() ?: return@mapNotNull null
        val trackUrl = track.string("url")?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        MediaSubtitle(trackUrl, track.string("label").orEmpty(), track.string("language").orEmpty(),
            track.string("mimeType") ?: "text/vtt",
            track.get("isDefault")?.let { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean && it.asBoolean } ?: false)
    }
    return ResolvedMedia(url, string("quality") ?: identity?.quality.orEmpty(),
        string("audio") ?: identity?.audio.orEmpty(), headers, backupUrls, string("mimeType"), subtitles)
}
