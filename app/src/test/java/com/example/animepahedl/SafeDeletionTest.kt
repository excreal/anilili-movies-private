package com.example.animepahedl

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SafeDeletionTest {
    @Test fun externalStorageChecksParentsWithoutLeavingGrantedFolder() {
        assertEquals(listOf("primary:Download/Movies/Show" to "primary:Download/Movies/Show/Video.mp4",
            "primary:Download/Movies" to "primary:Download/Movies/Show"),
            externalStorageAncestors("primary:Download/Movies", "primary:Download/Movies/Show/Video.mp4"))
        assertEquals(listOf("SD:" to "SD:Video.mp4"), externalStorageAncestors("SD:", "SD:Video.mp4"))
        assertTrue(externalStorageAncestors("primary:Download/Movies", "primary:Download/Movies 2/Video.mp4").isEmpty())
        assertTrue(externalStorageAncestors("primary:Download/Movies", "primary:Download/Movies/../Video.mp4").isEmpty())
        assertTrue(externalStorageAncestors("primary:Download/Movies", "primary:Download/Movies").isEmpty())
    }
    @Test fun matchesOnlySameBasenameSubtitleSidecarsIgnoringCase() {
        assertTrue(isOutputSubtitleSidecar("The Show - Episode 1.srt", "The Show - Episode 1.mp4"))
        assertTrue(isOutputSubtitleSidecar("the show - episode 1.VTT", "The Show - Episode 1.mp4"))
        assertTrue(isOutputSubtitleSidecar("The Show - Episode 1.en.vtt", "The Show - Episode 1.mp4"))
        assertTrue(isOutputSubtitleSidecar("The Show - Episode 1.zh-CN.vtt", "The Show - Episode 1.mp4"))
        assertFalse(isOutputSubtitleSidecar("The Show - Episode 2.srt", "The Show - Episode 1.mp4"))
        assertFalse(isOutputSubtitleSidecar("The Show - Episode 1.srt.bak", "The Show - Episode 1.mp4"))
        assertFalse(isOutputSubtitleSidecar("The Show - Episode 1.backup.vtt", "The Show - Episode 1.mp4"))
        assertFalse(isOutputSubtitleSidecar("The Show - Episode 1.jpg", "The Show - Episode 1.mp4"))
    }

    @Test fun subtitleSidecarNamesAreSafeAndLanguageSpecific() {
        assertEquals("The Show ep01.en.vtt", subtitleSidecarName("The Show ep01.mp4", "en", 0))
        assertEquals("The Show ep01.zh-cn.vtt", subtitleSidecarName("The Show ep01.mp4", "zh-CN", 1))
        assertEquals("The Show ep01.track-3.vtt", subtitleSidecarName("The Show ep01.mp4", "English!", 2))
    }

    @Test fun removesEmptyDownloadDirectoriesButKeepsRootAndNonemptyParents() {
        val root = kotlin.io.path.createTempDirectory("download-root-").toFile()
        try {
            val leaf = File(root, "Movies/Show").apply { mkdirs() }
            deleteEmptyDirectoriesUpTo(leaf, root)
            assertTrue(root.isDirectory)
            assertFalse(File(root, "Movies").exists())

            val nonempty = File(root, "Movies/Another Show").apply { mkdirs() }
            File(nonempty, "keep.mp4").writeText("video")
            deleteEmptyDirectoriesUpTo(nonempty, root)
            assertTrue(nonempty.isDirectory)
        } finally {
            root.deleteRecursively()
        }
    }
}
