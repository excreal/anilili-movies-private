package com.example.animepahedl

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class KisskhResilienceTest {
    @Test fun independentLocalizationFailuresKeepOriginalTracksAndInputOrder() = runBlocking {
        val tracks = (0 until 8).map { index ->
            MediaSubtitle("https://cdn.example/$index.srt", "Track $index", "lang$index")
        }
        val active = AtomicInteger()
        val peak = AtomicInteger()

        val result = bestEffortKisskhSubtitleLocalization(tracks) { track ->
            val now = active.incrementAndGet()
            peak.updateAndGet { maxOf(it, now) }
            try {
                val index = track.label.removePrefix("Track ").toInt()
                delay(((tracks.size - index) * 5).toLong()) // Force completions to arrive out of order.
                if (index == 3 || index == 6) throw IllegalStateException("Caption server unavailable")
                track.copy(url = "file:///captions/$index.vtt")
            } finally { active.decrementAndGet() }
        }

        val expected = tracks.mapIndexed { index, track ->
            if (index == 3 || index == 6) track else track.copy(url = "file:///captions/$index.vtt")
        }
        assertEquals(expected, result)
        assertTrue("Expected bounded parallel localization; observed peak ${peak.get()}", peak.get() in 2..3)
    }

    @Test fun subtitleLocalizationDoesNotSwallowCoroutineCancellation() {
        val track = MediaSubtitle("https://cdn.example/one.srt", "English", "en")

        assertThrows(CancellationException::class.java) {
            runBlocking {
                bestEffortKisskhSubtitleLocalization(listOf(track)) {
                    throw CancellationException("Cancelled")
                }
            }
        }
    }
}
