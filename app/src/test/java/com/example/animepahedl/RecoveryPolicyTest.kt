package com.example.animepahedl

import org.junit.Assert.*
import org.junit.Test

class RecoveryPolicyTest {
    @Test fun malformedStoredVerificationCannotTriggerAStartupFailureOrOpenANonHttpLink() {
        assertNull(parseMediaVerification("id", """{"id":"id","url":null,"browserUrl":"https://example.test/"}"""))
        assertNull(parseMediaVerification("id", """{"id":"id","url":"https://example.test/video","browserUrl":"file:///video"}"""))
        assertNull(parseMediaVerification("different", """{"id":"id","url":"https://example.test/video","browserUrl":"https://example.test/"}"""))
        assertEquals("https://example.test/", parseMediaVerification("id",
            """{"id":"id","url":"https://example.test/video","browserUrl":"https://example.test/"}""")?.browserUrl)
    }

    @Test fun expiredOneTimeLinksAreNotAutomaticallyResubmittedAsTransientNetworkFailures() {
        assertFalse(isTransientTransferHttpStatus(403))
        assertFalse(isTransientTransferHttpStatus(410))
        assertTrue(isTransientTransferHttpStatus(429))
        assertTrue(isTransientTransferHttpStatus(503))
        assertTrue(isTransientTransferHttpStatus(null))
    }
    @Test fun typingDefersChallengeUntilIdle() {
        val gate = SearchVerificationGate()
        val generation = gate.edit("nar", 0)
        gate.challenge(generation)
        assertFalse(gate.shouldPresent(400, true, true))
        assertFalse(gate.shouldPresent(1199, true, true))
        assertTrue(gate.shouldPresent(1200, true, true))
    }
    @Test fun editsInvalidateOldChallengesAndResetIdleTime() {
        val gate = SearchVerificationGate()
        val old = gate.edit("nar", 0)
        val latest = gate.edit("naruto", 800)
        gate.challenge(old)
        assertFalse(gate.shouldPresent(5000, true, true))
        gate.challenge(latest)
        assertFalse(gate.shouldPresent(1500, true, true))
        assertTrue(gate.shouldPresent(2000, true, true))
    }
    @Test fun clearingNavigationAndBackgroundPreventPresentation() {
        val gate = SearchVerificationGate()
        gate.challenge(gate.edit("one", 0))
        assertFalse(gate.shouldPresent(1300, false, true))
        assertFalse(gate.shouldPresent(1300, true, false))
        gate.invalidate()
        assertFalse(gate.shouldPresent(1300, true, true))
        gate.challenge(gate.edit("", 2000))
        assertFalse(gate.shouldPresent(4000, true, true))
    }
    @Test fun keyboardSubmitAllowsImmediatePresentation() {
        val gate = SearchVerificationGate()
        gate.challenge(gate.edit("one", 0))
        gate.submit()
        assertTrue(gate.shouldPresent(10, true, true))
    }
    @Test fun dismissalAndRepeatedChallengesCannotLoop() {
        val gate = SearchVerificationGate()
        val generation = gate.edit("one", 0)
        gate.challenge(generation)
        gate.presented()
        gate.challenge(generation)
        assertFalse(gate.shouldPresent(3000, true, true))
        gate.suppress()
        assertFalse(gate.shouldPresent(5000, true, true))
    }
    @Test fun exportStagesNeverOfferPauseOrTransferRetry() {
        for (stage in SaveState.entries.filter { it != SaveState.NONE }) {
            val row = DownloadRow("hls", "episode", TransferState.COMPLETE, 1f, save = stage)
            assertFalse(DownloadAction.PAUSE in row.actions)
            assertFalse(DownloadAction.RETRY in row.actions)
            assertEquals(stage == SaveState.FAILED, DownloadAction.RETRY_SAVE in row.actions)
        }
    }
    @Test fun recoveryEligibilityMatchesPersistedSourceContext() {
        val identity = DownloadIdentity(
            Anime("sites:DIRECT:clip", "Direct video", "", "", "Video", ""),
            Episode("clip", "1", "", ""), "Original", "und", "sites", "https://example.com/clip.mp4"
        )
        val media = ResolvedMedia("https://cdn.example.com/clip.mp4", "Original", "und", mimeType = "video/mp4")
        val directLink = ExportRecord("clip", "", "", identity = identity, media = media)
        assertEquals(DownloadRecoveryEligibility(retryable = true, transferControllable = true),
            downloadRecoveryEligibility(directLink))
        // A fresh source retry needs the locator, while pause/resume needs the persisted request context.
        assertEquals(DownloadRecoveryEligibility(retryable = true, transferControllable = false),
            downloadRecoveryEligibility(directLink.copy(media = null)))
        val legacy = directLink.copy(identity = identity.copy(source = null, locator = null))
        assertEquals(DownloadRecoveryEligibility(retryable = false, transferControllable = false),
            downloadRecoveryEligibility(legacy))
        assertEquals(DownloadRecoveryEligibility(retryable = false, transferControllable = false),
            downloadRecoveryEligibility(null))
    }
    @Test fun recoveryActionsAreEngineAndStateSpecific() {
        assertFalse(DownloadAction.RETRY in DownloadRow("direct:1", "episode", TransferState.FAILED, 0f).actions)
        assertFalse(DownloadAction.PAUSE in DownloadRow("direct:1", "episode", TransferState.DOWNLOADING, 0f).actions)
        assertTrue(DownloadAction.RESUME in DownloadRow("site-hls", "episode", TransferState.PAUSED, 0f,
            transferControllable = true).actions)
        assertTrue(DownloadAction.RESUME in DownloadRow("site-direct-video", "episode", TransferState.PAUSED, 0f,
            transferControllable = true).actions)
        assertTrue(DownloadAction.RETRY in DownloadRow("site-direct", "episode", TransferState.FAILED, 0f,
            retryable = true, transferControllable = true).actions)
        val orphanedLegacy = DownloadRow("legacy-hls", "episode", TransferState.PAUSED, 0f)
        assertFalse(DownloadAction.RESUME in orphanedLegacy.actions)
        assertFalse(DownloadAction.RETRY in orphanedLegacy.copy(transfer = TransferState.FAILED).actions)
        assertTrue(DownloadAction.DELETE in orphanedLegacy.actions)
        assertEquals(setOf(DownloadAction.DELETE), DownloadRow("hls", "episode", TransferState.DOWNLOADING, 0f, deleting = true).actions)
    }
    @Test fun operationIdentitiesCannotOverwriteOtherRecoveryActions() {
        val search = OperationId.Search(1)
        val download = OperationId.Download("hls")
        val failures = mutableMapOf(search to OperationFailure("search"), download to OperationFailure("download"))
        failures.remove(search)
        assertEquals("download", failures[download]?.message)
    }
    @Test fun rangesSupportSuffixOpenEndedAndClampedBounds() {
        assertNull(parseByteRange(null, 100))
        assertEquals(10L..19L, parseByteRange("bytes=10-19", 100))
        assertEquals(90L..99L, parseByteRange("bytes=90-", 100))
        assertEquals(90L..99L, parseByteRange("bytes=-10", 100))
        assertEquals(0L..99L, parseByteRange("bytes=-200", 100))
        assertEquals(90L..99L, parseByteRange("bytes=90-999", 100))
    }
    @Test fun invalidAndUnsatisfiableRangesAreRejected() {
        listOf("bytes=100-", "bytes=10-2", "bytes=-0", "bytes=-", "bytes=0-1,5-6", "garbage").forEach {
            try { parseByteRange(it, 100); fail(it) } catch (_: IllegalArgumentException) { }
        }
    }
}
