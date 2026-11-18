package com.anilili.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Test

class CastStreamRoutingTest {
    @Test
    fun refusedSidecarIsOmittedWithoutChangingStreamOrOtherTracks() {
        val item = itemWithSubtitles()
        val probes = CastStreamRouting.proxyProbeUrls(listOf(item), REFERER)

        assertEquals(3, probes.all().size)
        val streamProbe = requireNotNull(probes.stream)
        val acceptedSubtitleProbe = probes.subtitles.first()

        val cast = CastStreamRouting.castItems(
            listOf(item),
            REFERER,
            setOf(streamProbe, acceptedSubtitleProbe),
        ).single()

        assertEquals(streamProbe, cast.localConfiguration?.uri.toString())
        val tracks = cast.localConfiguration?.subtitleConfigurations.orEmpty()
        assertEquals(2, tracks.size)
        assertEquals(acceptedSubtitleProbe, tracks[0].uri.toString())
        assertEquals("https://subs.example/signs.ttml", tracks[1].uri.toString())
    }

    @Test
    fun refusedStreamKeepsDirectAddressWhileAcceptedSubtitleCanStillUseProxy() {
        val item = itemWithSubtitles()
        val probes = CastStreamRouting.proxyProbeUrls(listOf(item), REFERER)
        val subtitleProbe = probes.subtitles.first()

        val cast = CastStreamRouting.castItems(listOf(item), REFERER, setOf(subtitleProbe)).single()

        assertEquals("https://cdn.example/video.m3u8", cast.localConfiguration?.uri.toString())
        val tracks = cast.localConfiguration?.subtitleConfigurations.orEmpty()
        assertEquals(2, tracks.size)
        assertEquals(subtitleProbe, tracks.first().uri.toString())
        assertEquals("https://subs.example/signs.ttml", tracks.last().uri.toString())
    }

    private fun itemWithSubtitles(): MediaItem = MediaItem.Builder()
        .setMediaId("https://cdn.example/video.m3u8")
        .setUri("https://cdn.example/video.m3u8")
        .setSubtitleConfigurations(
            listOf(
                subtitle("https://subs.example/main.vtt", MimeTypes.TEXT_VTT),
                subtitle("https://refused.example/refused.vtt", MimeTypes.TEXT_VTT),
                subtitle("https://subs.example/signs.ttml", MimeTypes.APPLICATION_TTML),
            ),
        )
        .build()

    private fun subtitle(url: String, mimeType: String) =
        MediaItem.SubtitleConfiguration.Builder(Uri.parse(url)).setMimeType(mimeType).build()

    private companion object {
        const val REFERER = "https://watch.example/episode"
    }
}
