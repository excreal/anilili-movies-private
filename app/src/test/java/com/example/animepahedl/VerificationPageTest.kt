package com.example.animepahedl

import org.junit.Assert.assertEquals
import org.junit.Test

class VerificationPageTest {
    @org.junit.Test fun verificationMustFinishAtTheExpectedHttpsOrigin() {
        org.junit.Assert.assertTrue(verificationOriginMatches("https://example.org/start", "https://example.org:443/ready"))
        org.junit.Assert.assertFalse(verificationOriginMatches("https://example.org", "https://example.org:8443/ready"))
        org.junit.Assert.assertFalse(verificationOriginMatches("https://example.org", "https://other.example/ready"))
        org.junit.Assert.assertFalse(verificationOriginMatches("https://example.org", "http://example.org/ready"))
    }

    @Test fun apiChallengesOpenTheWebsite() {
        assertEquals("https://animepahe.pw/", verificationPage("https://animepahe.pw/api?m=search&q=test"))
        assertEquals("https://animepahe.pw/anime/show", verificationPage("https://animepahe.pw/api?m=release", "https://animepahe.pw/anime/show"))
        assertEquals("https://example.org/", VerificationRequired("https://example.org/api/v1").browserUrl)
    }
    @Test fun ordinaryPagesArePreservedAndForeignReferersAreIgnored() {
        assertEquals("https://example.org/play/one", verificationPage("https://example.org/play/one"))
        assertEquals("https://example.org/", verificationPage("https://example.org/api", "https://other.org/"))
    }
}
