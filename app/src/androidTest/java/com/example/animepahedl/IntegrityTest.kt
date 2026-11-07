package com.example.animepahedl

import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class IntegrityTest {
    @Test fun copyVerificationRejectsSameLengthCorruption() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "integrity-test-${System.nanoTime()}.bin")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Animepahe-test/")
        })!!
        val original = ByteArray(65536) { (it % 251).toByte() }
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(original)
        try {
            context.contentResolver.openOutputStream(uri)!!.use { it.write(original) }
            verifyCopy(context, uri, original.size.toLong(), digest)
            original[100] = (original[100] + 1).toByte()
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(original) }
            assertTrue(runCatching { verifyCopy(context, uri, original.size.toLong(), digest) }.isFailure)
        } finally { context.contentResolver.delete(uri, null, null) }
    }
    @Test fun silentVideoCannotSatisfyExpectedAudio() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = java.io.File(instrumentation.targetContext.cacheDir, "silent-test.mp4")
        try {
            instrumentation.context.assets.open("silent-video.mp4").use { input -> file.outputStream().use { input.copyTo(it) } }
            // The fixture has a one-second video track; presence is checked independently of duration.
            val executable = java.io.File(instrumentation.targetContext.applicationInfo.nativeLibraryDir, "libffmpeg_exec.so")
            val process = ProcessBuilder(executable.absolutePath, "-nostdin", "-v", "error", "-i", file.absolutePath,
                "-map", "0:v:0", "-map", "0:a:0", "-c", "copy", "-f", "null", "-").redirectErrorStream(true).start()
            try {
                val message = process.inputStream.bufferedReader().readText()
                assertNotEquals(message, 0, process.waitFor())
            } finally { if (process.isAlive) process.destroyForcibly() }
        } finally { file.delete() }
    }
}
