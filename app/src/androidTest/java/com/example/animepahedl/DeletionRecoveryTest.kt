package com.example.animepahedl

import android.content.*
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class DeletionRecoveryTest {
    @Test fun cancelKeepsAVideoThatFinishedBeforeConfirmation() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        DownloadStore.awaitReady()
        val id = "sites:cancel-finished-${System.nanoTime()}"
        val uri = Uri.parse("content://com.example.animepahedl.test.deletion/present-$id")
        try {
            Exports.put(app, id, ExportRecord("Finished before confirmation", "", "", uri = uri.toString()))
            DownloadStore.flush()
            val result = AndroidDownloadRepository(app).delete(id, deleteSavedFile = false)
            assertTrue(result.message, result.succeeded)
            assertFalse(Exports.all(app).containsKey(id))
            assertTrue(app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)!!.use { it.moveToFirst() })
        } finally {
            app.contentResolver.delete(uri, null, null)
            Exports.remove(app, id)
            DownloadStore.operation(id, "delete", null)
            DownloadStore.operation(id, "delete-keep-file", null)
            DownloadStore.flush()
        }
    }
    @Test fun missingStagingMediaOffersFreshSourceRetryInsteadOfRetrySave() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        DownloadStore.awaitReady()
        val id = "direct:${System.nanoTime()}"
        try {
            val identity = DownloadIdentity(
                Anime("sites:DIRECT:fixture", "Missing source fixture", "", "", "Video", ""),
                Episode("fixture", "1", "", ""), "Original", "und", "sites", "https://example.invalid/fixture.mp4"
            )
            Exports.put(app, id, ExportRecord("Missing source fixture", "", "", error = "Saving failed", identity = identity))
            DownloadStore.flush()
            val repository = AndroidDownloadRepository(app)
            withContext(Dispatchers.Main) { repository.refresh() }
            val row = repository.rows.value.first { it.id == id }
            assertTrue(DownloadAction.RETRY in row.actions)
            assertFalse(DownloadAction.RETRY_SAVE in row.actions)
        } finally { Exports.remove(app, id); DownloadStore.flush() }
    }

    @Test fun batchContinuesAfterProviderRefusalAndKeepsFailedMetadata() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext
        DownloadStore.awaitReady()
        val ids = listOf("delete", "missing", "refuse", "denied").associateWith { "direct:${System.nanoTime()}" }
        ids.forEach { (kind, id) -> Exports.put(app, id, ExportRecord("Deletion fixture", "", "", uri = "content://com.example.animepahedl.test.deletion/$kind-${System.nanoTime()}")) }
        DownloadStore.flush()
        val repository = AndroidDownloadRepository(app)
        try {
            val results = withContext(Dispatchers.Main) { repository.deleteBatch(ids.values.toSet()) }
            assertEquals(2, results.count { it.succeeded })
            assertFalse(Exports.all(app).containsKey(ids.getValue("delete")))
            assertFalse(Exports.all(app).containsKey(ids.getValue("missing")))
            listOf("refuse", "denied").forEach { kind ->
                assertTrue(Exports.all(app).containsKey(ids.getValue(kind)))
                assertTrue(Exports.deleting(ids.getValue(kind)))
            }
        } finally { ids.values.forEach { Exports.remove(app, it); DownloadStore.operation(it, "delete", null) }; DownloadStore.flush() }
    }
    @Test fun legacyImportIsRepeatableAndNeverOverwritesSavedDestinations() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        withContext(Dispatchers.IO) {
            val db = androidx.room.Room.inMemoryDatabaseBuilder(context, DownloadDatabase::class.java).build()
            try {
                val dao = db.records()
                val record = ExportRecord("Saved", "tree", "folder", "content://saved/video", bytes = 123)
                val json = com.google.gson.Gson().toJson(record)
                repeat(2) { dao.importLegacy(StoredDownload("existing-id", json)) }
                dao.importLegacy(StoredDownload("existing-id", "{}"))
                assertEquals(1, dao.downloads().size)
                assertEquals(json, dao.downloads().single().metadata)
            } finally { db.close() }
        }
    }
}
