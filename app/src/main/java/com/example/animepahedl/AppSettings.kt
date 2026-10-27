package com.example.animepahedl

import android.content.Context

/** DownloadManager destinations must stay inside the public Downloads directory. */
internal fun normalizeOutputFolder(input: String): String? {
    val path = input.trim().trim('/')
    if (path.isEmpty()) return ""
    val segments = path.split('/')
    if (segments.any { it.isBlank() || it == "." || it == ".." || it.length > 80 ||
        it.any { char -> char.code < 32 || char in "\\:*?\"<>|" } }) return null
    return path.takeIf { it.length <= 180 }
}

internal class AppSettings(context: Context) {
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var defaultAudio: String
        get() = preferences.getString("default-audio", "Original").orEmpty()
        set(value) { preferences.edit().putString("default-audio", value).apply() }
    var connections: Int
        get() = preferences.getInt("hls-connections", 8).coerceIn(2, 12)
        set(value) { preferences.edit().putInt("hls-connections", value.coerceIn(2, 12)).apply() }
    var outputTree: String
        get() = preferences.getString("output-tree", "").orEmpty()
        set(value) { preferences.edit().putString("output-tree", value).apply() }
    var outputFolder: String
        get() = preferences.getString("output-folder", "Animepahe").orEmpty()
        set(value) { preferences.edit().putString("output-folder", value).apply() }
}
