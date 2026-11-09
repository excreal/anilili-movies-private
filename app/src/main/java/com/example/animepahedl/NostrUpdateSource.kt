package com.example.animepahedl

import fr.acinq.secp256k1.Secp256k1
import java.security.MessageDigest
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/** Anilili's signed Nostr transport, scoped to this app's announcement identity.
 * Update-manifest fetching is omitted; verification and relay reads retain the source logic.
 */
object NostrUpdateSource {
    const val MANIFEST_PUBKEY_HEX = "bb8a9ce60cacac6ee3bfefdc272df9fbc21a8010e2eb61b96aa3558ab97e71b5"
    const val MANIFEST_KIND = 30078

    const val MANIFEST_D_TAG = "video-downloader-update"

    private val RELAYS = listOf(
        "wss://relay.damus.io",
        "wss://nos.lol",
        "wss://relay.nostr.band",
        "wss://relay.primal.net",
    )
    private const val SUBSCRIPTION_ID = "video-downloader-update"
    private val PER_RELAY_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10)
    private val OVERALL_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(15)
    internal const val MAX_RELAY_FRAME_BYTES = 512 * 1024
    internal const val MAX_EVENTS_PER_RELAY = 16
    internal const val MAX_FRAMES_PER_RELAY = 64

    /**
     * The same signed event, served as a file from the APK mirror.
     *
     * Relays are the durable copy but they are slow and flaky — several time out on every publish,
     * and the scan waits up to 15 s for the slowest. A CDN answers in one request, so this is
     * tried first and the relays become the fallback rather than the front door.
     *
     * Security is unchanged. What is fetched is the identical signed Nostr event, not a plain
     * JSON of convenience, so it goes through the same [NostrEvent.isValidManifest] check: project
     * pubkey, kind, d-tag, id hash and BIP-340 signature. Whoever controls the mirror can withhold
     * an update or serve an older signed one, but cannot forge one, and cannot point the app at an
     * APK the project did not sign — Android refuses an install signed with another key.
     */
    internal fun scanRelays(
        client: OkHttpClient,
        json: Json,
        dTag: String,
        subscriptionId: String,
    ): RelayScanResult {
        val events = ConcurrentLinkedQueue<JsonObject>()
        val answered = java.util.concurrent.atomic.AtomicInteger(0)
        val remaining = CountDownLatch(RELAYS.size)
        RELAYS.forEach { relay ->
            thread(name = "nostr-$subscriptionId", isDaemon = true) {
                try {
                    if (queryRelay(client, relay, dTag, subscriptionId, events, json)) answered.incrementAndGet()
                } catch (error: Exception) {
                    DiagnosticsLog.event("Nostr relay failed relay=$relay dTag=$dTag reason=${error.message}")
                } finally {
                    remaining.countDown()
                }
            }
        }
        remaining.await(OVERALL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        val candidates = events.mapNotNull { raw ->
            runCatching { raw.toNostrEvent() }.getOrNull()
        }
        val verified = candidates.filter { it.isValidSignedBy(MANIFEST_PUBKEY_HEX, MANIFEST_KIND, dTag) }
        DiagnosticsLog.event(
            "Nostr scan complete dTag=$dTag relays=${RELAYS.size} events=${candidates.size} " +
                "verified=${verified.size}",
        )
        return RelayScanResult(verified, answered.get(), candidates.size)
    }

    private fun queryRelay(
        client: OkHttpClient,
        relay: String,
        dTag: String,
        subscriptionId: String,
        sink: ConcurrentLinkedQueue<JsonObject>,
        json: Json,
    ): Boolean {
        val request = buildJsonArray {
            add(JsonPrimitive("REQ"))
            add(JsonPrimitive(subscriptionId))
            add(
                JsonObject(
                    mapOf(
                        "kinds" to buildJsonArray { add(JsonPrimitive(MANIFEST_KIND)) },
                        "authors" to buildJsonArray { add(JsonPrimitive(MANIFEST_PUBKEY_HEX)) },
                        "#d" to buildJsonArray { add(JsonPrimitive(dTag)) },
                        "limit" to JsonPrimitive(3),
                    ),
                ),
            )
        }
        val answered = java.util.concurrent.atomic.AtomicBoolean(false)
        val gate = RelayFrameGate(subscriptionId, json)
        val done = CountDownLatch(1)
        val webSocket = client.newWebSocket(
            Request.Builder().url(relay).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(json.encodeToString(JsonElement.serializer(), request))
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    // Check frame size before UTF-8 conversion and JSON parsing. OkHttp has
                    // already assembled the text frame, but this prevents our parser from
                    // allocating a second unbounded JSON tree from relay-controlled data.
                    val decision = gate.accept(text)
                    when (val frame = decision.frame) {
                        is RelayFrame.Event -> {
                            sink.add(frame.event)
                        }
                        RelayFrame.EndOfStoredEvents -> {
                            answered.set(true)
                        }
                        null -> Unit
                    }
                    if (decision.shouldClose) {
                        // We have enough bounded candidates or received a terminal/invalid limit.
                        // Stop before any subsequent relay frame can be parsed or queued.
                        if (decision.frame is RelayFrame.Event) answered.set(true)
                        webSocket.close(
                            if (decision.frame == null) 1009 else 1000,
                            if (decision.frame == null) "Relay response limit exceeded" else null,
                        )
                        done.countDown()
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    DiagnosticsLog.event("Announcement relay connection failed: ${t.javaClass.simpleName}")
                    done.countDown()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    done.countDown()
                }
            },
        )
        done.await(PER_RELAY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        runCatching { webSocket.close(1000, null) }
        return answered.get()
    }
}

/** Only the two NIP-01 response messages for this exact subscription are actionable. */
internal sealed interface RelayFrame {
    data class Event(val event: JsonObject) : RelayFrame
    data object EndOfStoredEvents : RelayFrame
}

internal data class RelayFrameDecision(val frame: RelayFrame?, val shouldClose: Boolean)

/** Bounds untrusted frames, parsing work and retained event count for one relay connection. */
internal class RelayFrameGate(
    private val subscriptionId: String,
    private val json: Json,
    private val maxFrames: Int = NostrUpdateSource.MAX_FRAMES_PER_RELAY,
    private val maxEvents: Int = NostrUpdateSource.MAX_EVENTS_PER_RELAY,
) {
    private var frameCount = 0
    private var eventCount = 0
    private var closed = false

    fun accept(text: String): RelayFrameDecision {
        if (closed) return RelayFrameDecision(null, true)
        frameCount++
        if (frameCount > maxFrames || text.length > NostrUpdateSource.MAX_RELAY_FRAME_BYTES ||
            text.toByteArray(Charsets.UTF_8).size > NostrUpdateSource.MAX_RELAY_FRAME_BYTES
        ) {
            closed = true
            return RelayFrameDecision(null, true)
        }
        val frame = decodeRelayFrame(text, subscriptionId, json)
        if (frame is RelayFrame.Event) {
            eventCount++
            if (eventCount >= maxEvents) closed = true
            return RelayFrameDecision(frame, closed)
        }
        if (frame == RelayFrame.EndOfStoredEvents) closed = true
        return RelayFrameDecision(frame, closed)
    }
}

internal fun decodeRelayFrame(text: String, subscriptionId: String, json: Json): RelayFrame? {
    if (text.length > NostrUpdateSource.MAX_RELAY_FRAME_BYTES ||
        text.toByteArray(Charsets.UTF_8).size > NostrUpdateSource.MAX_RELAY_FRAME_BYTES
    ) return null
    return runCatching {
        val message = json.parseToJsonElement(text).jsonArray
        when {
            message.size == 3 && message[0].jsonPrimitive.content == "EVENT" &&
                message[1].jsonPrimitive.content == subscriptionId ->
                RelayFrame.Event(message[2].jsonObject)
            message.size == 2 && message[0].jsonPrimitive.content == "EOSE" &&
                message[1].jsonPrimitive.content == subscriptionId -> RelayFrame.EndOfStoredEvents
            else -> null
        }
    }.getOrNull()
}

internal data class NostrEvent(
    val id: String,
    val pubkey: String,
    val createdAt: Long,
    val kind: Int,
    val tags: JsonArray,
    val content: String,
    val sig: String,
) {
    /** Full NIP-01 verification of the update manifest: shape, d-tag, id hash, Schnorr signature. */
    fun isValidManifest(): Boolean = isValidSignedBy(
        NostrUpdateSource.MANIFEST_PUBKEY_HEX,
        NostrUpdateSource.MANIFEST_KIND,
        NostrUpdateSource.MANIFEST_D_TAG,
    )

    /**
     * The same verification for any of the project's addressable events.
     *
     * The d-tag is what separates one channel from another under a single key — the update
     * manifest and the announcement live at the same kind and pubkey — so it is checked here
     * rather than by the caller. An event that verifies is one this project signed; nothing about
     * a relay is trusted, and a relay can therefore only ever withhold, never forge.
     */
    fun isValidSignedBy(expectedPubkeyHex: String, expectedKind: Int, expectedDTag: String): Boolean {
        if (!pubkey.equals(expectedPubkeyHex, ignoreCase = true)) return false
        if (kind != expectedKind) return false
        val hasDTag = tags.filterIsInstance<JsonArray>().any { tag ->
            tag.size >= 2 &&
                tag[0].jsonPrimitive.content == "d" &&
                tag[1].jsonPrimitive.content == expectedDTag
        }
        if (!hasDTag) return false
        val expectedId = computeEventIdHex()
        if (!expectedId.equals(id, ignoreCase = true)) {
            DiagnosticsLog.event("Nostr event rejected dTag=$expectedDTag reason=id-mismatch")
            return false
        }
        val sigBytes = runCatching { sig.hexToBytes() }.getOrNull() ?: return false
        val idBytes = expectedId.hexToBytes()
        val pubBytes = pubkey.hexToBytes()
        if (sigBytes.size != 64 || idBytes.size != 32 || pubBytes.size != 32) return false
        // Distinguish "this event is not genuine" from "this build cannot check signatures at
        // all". They are the same `false` to the caller, and conflating them hid a real defect
        // for an entire release: R8 shrank the secp256k1 JNI backend away, the verifier threw
        // ExceptionInInitializerError on first touch, and every manifest silently looked forged.
        return runCatching { Secp256k1.verifySchnorr(sigBytes, idBytes, pubBytes) }
            .onFailure { DiagnosticsLog.throwable("Nostr signature check unavailable", it) }
            .onSuccess { if (!it) DiagnosticsLog.event("Nostr event rejected dTag=$expectedDTag reason=bad-signature") }
            .getOrDefault(false)
    }

    /** NIP-01 id: sha256 of the compact JSON array [0, pubkey, created_at, kind, tags, content]. */
    fun computeEventIdHex(): String {
        val preimage = buildJsonArray {
            add(JsonPrimitive(0))
            add(JsonPrimitive(pubkey))
            add(JsonPrimitive(createdAt))
            add(JsonPrimitive(kind))
            add(tags)
            add(JsonPrimitive(content))
        }
        val serialized = NostrJson.encodeToString(JsonElement.serializer(), preimage)
        val digest = MessageDigest.getInstance("SHA-256").digest(serialized.toByteArray(Charsets.UTF_8))
        return digest.toHexString()
    }

    /**
     * Reads the manifest, preferring this device's form factor.
     *
     * Since the mobile/TV flavor split a release ships two different APKs, but the manifest's
     * top-level `apkUrl` can only name one — and it names the phone build, which is what every
     * pre-split install is already following. A `variants` object carries the rest:
     *
     *     {"version":…, "apkUrl":…, "variants":{"tv":{"url":…,"sizeBytes":…}}}
     *
     * A TV takes `variants.tv` when the publisher provided it and otherwise falls back to
     * `apkUrl`, so an older manifest still updates a TV (with the phone build, exactly as it did
     * before the split) rather than leaving it stranded. Apps older than this simply ignore
     * `variants`, which is why the top-level fields stay put.
     */
}

internal fun JsonObject.toNostrEvent(): NostrEvent = NostrEvent(
    id = this["id"]?.jsonPrimitive?.content ?: error("event missing id"),
    pubkey = this["pubkey"]?.jsonPrimitive?.content ?: error("event missing pubkey"),
    createdAt = this["created_at"]?.jsonPrimitive?.content?.toLongOrNull() ?: error("event missing created_at"),
    kind = this["kind"]?.jsonPrimitive?.content?.toIntOrNull() ?: error("event missing kind"),
    tags = this["tags"]?.jsonArray ?: JsonArray(emptyList()),
    content = this["content"]?.jsonPrimitive?.content ?: "",
    sig = this["sig"]?.jsonPrimitive?.content ?: "",
)

private val NostrJson = Json

internal fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

internal fun String.hexToBytes(): ByteArray {
    require(length % 2 == 0) { "odd-length hex" }
    return ByteArray(length / 2) { i ->
        ((Character.digit(this[i * 2], 16) shl 4) or Character.digit(this[i * 2 + 1], 16)).toByte()
    }
}

internal data class RelayScanResult(val events: List<NostrEvent>, val answered: Int, val received: Int)
