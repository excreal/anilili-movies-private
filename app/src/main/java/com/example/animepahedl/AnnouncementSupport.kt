package com.example.animepahedl
internal object AnnouncementHttp {
    val client = okhttp3.OkHttpClient.Builder().connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS).build()
}
internal object DiagnosticsLog {
    fun event(message: String) { android.util.Log.d("Announcements", message) }
    fun throwable(message: String, failure: Throwable) { android.util.Log.w("Announcements", message, failure) }
}
