package com.example.animepahedl

import java.io.IOException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncementInputBoundsTest {
    @Test fun boundedReaderAcceptsSmallChunkedMirrorBody() {
        withMockBody(MockResponse().setChunkedBody("{\"id\":\"ok\"}", 2)) { body ->
            assertEquals("{\"id\":\"ok\"}", readBoundedAnnouncementBody(body))
        }
    }

    @Test fun boundedReaderRejectsDeclaredOversizeBeforeReading() {
        withMockBody(MockResponse().setBody("x".repeat(AnnouncementSource.MAX_MIRROR_BYTES + 1))) { body ->
            assertTrue(runCatching { readBoundedAnnouncementBody(body) }.exceptionOrNull() is IOException)
        }
    }

    @Test fun boundedReaderRejectsChunkedOversizeWithoutContentLength() {
        withMockBody(
            MockResponse().setChunkedBody("x".repeat(AnnouncementSource.MAX_MIRROR_BYTES + 1), 4096),
        ) { body ->
            assertTrue(runCatching { readBoundedAnnouncementBody(body) }.exceptionOrNull() is IOException)
        }
    }

    @Test fun relayFrameRequiresExpectedSubscriptionAndExactNipMessageShape() {
        val event = """{"id":"event"}"""
        assertEquals(
            RelayFrame.Event(Json.parseToJsonElement(event).let { it as kotlinx.serialization.json.JsonObject }),
            decodeRelayFrame("[\"EVENT\",\"expected\",$event]", "expected", Json),
        )
        assertNull(decodeRelayFrame("[\"EVENT\",\"other\",$event]", "expected", Json))
        assertNull(decodeRelayFrame("[\"EOSE\",\"other\"]", "expected", Json))
        assertEquals(RelayFrame.EndOfStoredEvents, decodeRelayFrame("[\"EOSE\",\"expected\"]", "expected", Json))
        assertNull(decodeRelayFrame("[\"EVENT\",\"expected\",$event,\"extra\"]", "expected", Json))
        assertNull(decodeRelayFrame("not json", "expected", Json))
    }

    @Test fun relayGateRejectsOversizeBeforeJsonDecode() {
        val gate = RelayFrameGate("expected", Json)
        val decision = gate.accept(" ".repeat(NostrUpdateSource.MAX_RELAY_FRAME_BYTES + 1))
        assertNull(decision.frame)
        assertTrue(decision.shouldClose)
    }

    @Test fun relayGateStopsAfterEventLimitAndDoesNotRetainFlood() {
        val gate = RelayFrameGate("expected", Json, maxFrames = 100, maxEvents = 2)
        val frame = "[\"EVENT\",\"expected\",{\"id\":\"event\"}]"
        val first = gate.accept(frame)
        assertTrue(first.frame is RelayFrame.Event)
        assertFalse(first.shouldClose)
        val second = gate.accept(frame)
        assertTrue(second.frame is RelayFrame.Event)
        assertTrue(second.shouldClose)
        val flood = gate.accept(frame)
        assertNull(flood.frame)
        assertTrue(flood.shouldClose)
    }

    private fun withMockBody(response: MockResponse, block: (okhttp3.ResponseBody) -> Unit) {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(response)
            OkHttpClient().newCall(Request.Builder().url(server.url("/notice")).build()).execute().use { fetched ->
                block(requireNotNull(fetched.body))
            }
        }
    }
}
