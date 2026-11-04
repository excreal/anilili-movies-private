package com.example.animepahedl

import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class ExistingOutputTest {
    @Test fun publishedOutputMatchesFolderQualityAndLanguage() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = "Animepahe-test-${System.currentTimeMillis()}"
        val english = downloadLocation("Fixture Show", "1", "1080p", "mp4", "eng")
        val japanese = downloadLocation("Fixture Show", "1", "1080p", "mp4", "jpn")
        val lowQuality = downloadLocation("Fixture Show", "1", "720p", "mp4", "eng")
        val uri = app.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, english.fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$folder/${english.showFolder}/")
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }) ?: error("Could not create test file")
        try {
            assertTrue(existingOutput(app, "", folder, english))
            assertFalse(existingOutput(app, "", folder, japanese))
            assertFalse(existingOutput(app, "", folder, lowQuality))
            assertFalse(existingOutput(app, "", "$folder-other", english))
        } finally { app.contentResolver.delete(uri, null, null) }
    }
}
