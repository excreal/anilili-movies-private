package com.example.animepahedl

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File

/** Bounded local diagnostics shared by release playback and download/export stages. */
internal object DebugTrace {
    private var file: File? = null
    private val writer = java.util.concurrent.Executors.newSingleThreadExecutor()
    fun initialize(context: Context) {
        file = File(context.getExternalFilesDir(null) ?: context.filesDir, "captures/diagnostics.jsonl")
            .also { it.parentFile?.mkdirs() }
        event("app_started", "version" to BuildConfig.VERSION_NAME)
    }

    fun safeUrl(url: String): String = url.toHttpUrlOrNull()?.newBuilder()?.username("")?.password("")?.encodedPath("/")?.query(null)?.fragment(null)?.build()?.toString() ?: "invalid-url"

    fun sanitize(value: String): String = value.take(4096)
        .replace(Regex("https?://[^\\s\"<>]+")) { safeUrl(it.value) }
        .replace(Regex("(?i)(cookie|authorization|password)\\s*[:=][^\\r\\n]*"), "$1=[redacted]")

    @Synchronized
    fun event(name: String, vararg values: Pair<String, Any?>) {
        val destination = file ?: return
        val line = Gson().toJson(mapOf("time" to java.time.Instant.now().toString(), "epochMs" to System.currentTimeMillis(), "event" to name) + values.associate { (key, value) -> key to if (key.contains("cookie", true) || key.contains("authorization", true) || key.contains("password", true)) "redacted" else if (value is String) sanitize(value) else value })
        Log.d("AnililiMoviesHTTP", line)
        writer.execute {
            runCatching {
                if (destination.length() > 4_000_000) {
                    val tail = java.io.RandomAccessFile(destination, "r").use { input ->
                        input.seek((input.length() - 2_000_000).coerceAtLeast(0))
                        input.readLine()
                        ByteArray((input.length() - input.filePointer).toInt()).also { input.readFully(it) }
                    }
                    destination.writeBytes(tail)
                }
                destination.appendText("$line\n")
            }
        }
    }
}
