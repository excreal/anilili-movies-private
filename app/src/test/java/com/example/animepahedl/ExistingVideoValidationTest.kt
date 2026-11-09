package com.example.animepahedl

import org.junit.Assert.*
import org.junit.Test

class ExistingVideoValidationTest {
    @Test fun validatesContainerSignatureAndMinimumSize() {
        assertFalse(isPlausibleExistingVideo(0, byteArrayOf()))
        assertFalse(isPlausibleExistingVideo(500, "ftyp".toByteArray()))
        assertFalse(isPlausibleExistingVideo(10_000, ByteArray(16)))
        assertTrue(isPlausibleExistingVideo(10_000, byteArrayOf(0, 0, 0, 24, 102, 116, 121, 112)))
        assertTrue(isPlausibleExistingVideo(10_000, byteArrayOf(0x1a, 0x45, 0xdf.toByte(), 0xa3.toByte())))
        assertTrue(isPlausibleExistingVideo(10_000, byteArrayOf(0, 0, 0, 24, 109, 111, 111, 118)))
    }
}
