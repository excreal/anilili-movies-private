package com.example.animepahedl

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong

internal fun downloadsPendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
    context, 0, Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_DOWNLOADS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

/** Aggregate network traffic for the app's HLS downloads, sampled when notifications refresh. */
internal object DownloadSpeed {
    private val transferred = AtomicLong()
    private var lastBytes = 0L
    private var lastTime = 0L
    @Volatile private var lastRate = 0L
    fun add(bytes: Int) { if (bytes > 0) transferred.addAndGet(bytes.toLong()) }
    @Synchronized fun sample(now: Long = SystemClock.elapsedRealtime()): Long {
        val bytes = transferred.get()
        if (lastTime == 0L) { lastTime = now; lastBytes = bytes; return 0 }
        val elapsed = now - lastTime
        if (elapsed >= 500) {
            lastRate = ((bytes - lastBytes).coerceAtLeast(0) * 1000 / elapsed)
            lastTime = now
            lastBytes = bytes
        }
        return lastRate
    }
    @Synchronized fun reset() { lastBytes = transferred.get(); lastTime = 0; lastRate = 0 }
}
