package com.example.animepahedl

import org.junit.Assert.*
import org.junit.Test

class HlsVariantSelectionTest {
    private val master = """
        #EXTM3U
        #EXT-X-VERSION:7
        #EXT-X-SESSION-KEY:METHOD=AES-128,URI="key.bin"
        #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="low",NAME="Low",URI="audio-low.m3u8"
        #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="high",NAME="High",URI="audio-high.m3u8"
        #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360,AUDIO="low"
        low.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=4000000,RESOLUTION=1920x1080,AUDIO="high"
        high.m3u8
    """.trimIndent()
    @Test fun bestPreservesSelectedAudioAndSessionKey() {
        val selected = selectCachedHlsVariant(master, "Best")
        assertTrue(selected.contains("\nhigh.m3u8"))
        assertTrue(selected.contains("audio-high.m3u8"))
        assertTrue(selected.contains("SESSION-KEY"))
        assertFalse(selected.contains("low.m3u8"))
        assertFalse(selected.contains("audio-low.m3u8"))
    }
    @Test fun qualityLimitKeepsMatchingRenditions() {
        val selected = selectCachedHlsVariant(master, "720p")
        assertTrue(selected.contains("\nlow.m3u8"))
        assertTrue(selected.contains("audio-low.m3u8"))
        assertFalse(selected.contains("high.m3u8"))
    }
    @Test fun mediaPlaylistsRemainUnchanged() {
        val media = "#EXTM3U\n#EXTINF:3,\nsegment.ts\n#EXT-X-ENDLIST\n"
        assertEquals(media, selectCachedHlsVariant(media, "Best"))
    }
    @Test fun bandwidthSelectsUnknownResolutionAndMalformedMasterFails() {
        val unknown = "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=10\nlow.m3u8\n#EXT-X-STREAM-INF:BANDWIDTH=20\nhigh.m3u8"
        assertTrue(selectCachedHlsVariant(unknown, "Best").contains("high.m3u8"))
        assertTrue(runCatching { selectCachedHlsVariant("#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=10", "Best") }.isFailure)
    }
}
