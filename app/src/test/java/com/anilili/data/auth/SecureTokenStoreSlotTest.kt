package com.anilili.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecureTokenStoreSlotTest {
    @Test fun onlyAniListSlotOwnsLegacyTokenMigrationKey() {
        assertEquals(
            SecureTokenStore.KEY_TOKEN_LEGACY,
            legacyTokenKeyForSlot(SecureTokenStore.KEY_TOKEN_ENCRYPTED),
        )
        assertNull(legacyTokenKeyForSlot(SecureTokenStore.KEY_OPEN_SUBTITLES_CREDENTIALS))
    }
}
