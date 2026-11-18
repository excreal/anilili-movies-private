package com.example.animepahedl

internal data class FfmpegProgress(val elapsedUs: Long = -1, val bytes: Long = 0, val percent: Int = -1)

/** Consume complete FFmpeg progress records. Only a validated output can reach 100%. */
internal class FfmpegProgressParser(private val durationUs: Long) {
    private var timestampUs: Long? = null
    private var legacyTimestampUs: Long? = null
    private var formattedTimestampUs: Long? = null
    private var bytes: Long? = null
    private var previous = FfmpegProgress()

    fun accept(line: String): FfmpegProgress? {
        val value = line.substringAfter('=', "").trim()
        when (line.substringBefore('=')) {
            "out_time_us" -> timestampUs = value.toLongOrNull()?.takeIf { it >= 0 }
            // FFmpeg's legacy out_time_ms field also contains microseconds.
            "out_time_ms" -> legacyTimestampUs = value.toLongOrNull()?.takeIf { it >= 0 }
            "out_time" -> formattedTimestampUs = formattedTimeUs(value)
            "total_size" -> bytes = value.toLongOrNull()?.takeIf { it >= 0 }
            "progress" -> if (value == "continue" || value == "end") {
                val elapsed = maxOf(previous.elapsedUs, timestampUs ?: legacyTimestampUs ?: formattedTimestampUs ?: -1)
                val written = maxOf(previous.bytes, bytes ?: 0)
                val percent = if (durationUs > 0 && elapsed >= 0)
                    (elapsed.toDouble() * 100 / durationUs).toInt().coerceIn(0, 99) else -1
                timestampUs = null
                legacyTimestampUs = null
                formattedTimestampUs = null
                bytes = null
                val next = FfmpegProgress(elapsed, written, percent)
                if (next != previous) {
                    previous = next
                    return next
                }
            }
        }
        return null
    }

    private fun formattedTimeUs(value: String): Long? {
        val match = clock.matchEntire(value) ?: return null
        val hours = match.groupValues[1].toLongOrNull() ?: return null
        val minutes = match.groupValues[2].toLong()
        val seconds = match.groupValues[3].toLong()
        val fractionUs = match.groupValues[4].take(6).padEnd(6, '0').toLong()
        val remainder = (minutes * 60 + seconds) * 1_000_000 + fractionUs
        return if (hours <= (Long.MAX_VALUE - remainder) / 3_600_000_000L) hours * 3_600_000_000L + remainder else null
    }

    private companion object { val clock = Regex("""(\d+):([0-5]\d):([0-5]\d)(?:\.(\d+))?""") }
}
