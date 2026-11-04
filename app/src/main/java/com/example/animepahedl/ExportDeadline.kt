package com.example.animepahedl

import kotlinx.coroutines.*

/** Stage deadline plus a no-progress watchdog; advancing copies get up to thirty minutes. */
internal suspend fun <T> withExportProgressDeadline(id: String, block: suspend () -> T): T = withTimeout(30 * 60_000L) {
    coroutineScope {
        val worker = async { block() }
        val watchdog = launch {
            var previous = Exports.progress[id]
            var changedAt = android.os.SystemClock.elapsedRealtime()
            while (worker.isActive) {
                delay(1000)
                val current = Exports.progress[id]
                if (current != previous) { previous = current; changedAt = android.os.SystemClock.elapsedRealtime() }
                if (android.os.SystemClock.elapsedRealtime() - changedAt > 120_000) {
                    throw java.io.IOException("Saving made no progress for two minutes. Retry save; the source is retained.")
                }
            }
        }
        try { worker.await() } finally { watchdog.cancel() }
    }
}
