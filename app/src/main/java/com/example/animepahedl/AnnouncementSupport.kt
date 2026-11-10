package com.example.animepahedl
internal object AnnouncementHttp {
    val client = okhttp3.OkHttpClient.Builder().connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS).build()
}
internal object DiagnosticsLog {
    fun event(message: String) { DebugTrace.event("announcement", "message" to message) }
    fun throwable(message: String, failure: Throwable) { DebugTrace.event("announcement_failure", "message" to message, "type" to failure.javaClass.simpleName) }
}
