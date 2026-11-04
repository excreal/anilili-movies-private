package com.example.animepahedl

import android.content.Context
import androidx.room.*
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

@Entity(tableName = "downloads")
internal data class StoredDownload(@PrimaryKey val id: String, val metadata: String)
@Entity(tableName = "operations", primaryKeys = ["id", "kind"])
internal data class PendingOperation(val id: String, val kind: String, val payload: String)
@Dao
internal interface DownloadDao {
    @Query("SELECT * FROM downloads") fun downloads(): List<StoredDownload>
    @Query("SELECT * FROM operations") fun operations(): List<PendingOperation>
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun put(value: StoredDownload)
    @Insert(onConflict = OnConflictStrategy.IGNORE) fun importLegacy(value: StoredDownload)
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun put(value: PendingOperation)
    @Query("DELETE FROM downloads WHERE id = :id") fun remove(id: String)
    @Query("DELETE FROM operations WHERE id = :id AND kind = :kind") fun removeOperation(id: String, kind: String)
}
@Database(entities = [StoredDownload::class, PendingOperation::class], version = 1, exportSchema = true)
internal abstract class DownloadDatabase : RoomDatabase() { abstract fun records(): DownloadDao }

/** Ordered disk writes and an in-memory snapshot keep all database access off the UI thread.
 * Critical transitions await [flush] before stopping engines or publishing media.
 */
internal object DownloadStore {
    private val writer = Executors.newSingleThreadExecutor()
    private val gson = Gson()
    private val records = ConcurrentHashMap<String, ExportRecord>()
    private val operations = ConcurrentHashMap<Pair<String, String>, String>()
    private val writes = OrderedWrites()
    val revisions = kotlinx.coroutines.flow.MutableStateFlow(0L)
    val health = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val ready = CompletableDeferred<Unit>()
    private var started = false
    private lateinit var database: DownloadDatabase
    @Synchronized fun initialize(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        writer.execute {
            try {
                database = Room.databaseBuilder(app, DownloadDatabase::class.java, "downloads.db").build()
                val dao = database.records()
                // The marker and imports commit in one transaction. Existing Room rows always win.
                database.runInTransaction {
                    if (dao.operations().none { it.id == "legacy" && it.kind == "migration" }) {
                        app.getSharedPreferences("exports", Context.MODE_PRIVATE).all.forEach { (id, value) ->
                            runCatching { gson.fromJson(value.toString(), ExportRecord::class.java) }.getOrNull()?.let {
                                dao.importLegacy(StoredDownload(id, gson.toJson(it)))
                            }
                        }
                        app.getSharedPreferences("direct-downloads", Context.MODE_PRIVATE).all.forEach { (id, title) ->
                            dao.put(PendingOperation(id, "direct", title.toString()))
                        }
                        dao.put(PendingOperation("legacy", "migration", "1"))
                    }
                }
                dao.downloads().forEach { row ->
                    runCatching { gson.fromJson(row.metadata, ExportRecord::class.java) }.getOrNull()?.let { records.putIfAbsent(row.id, it) }
                }
                dao.operations().forEach { operations.putIfAbsent(it.id to it.kind, it.payload) }
                ready.complete(Unit)
            } catch (e: Exception) { ready.completeExceptionally(e) }
        }
    }
    suspend fun awaitReady() = ready.await()
    fun all(): Map<String, ExportRecord> = records.toMap()
    fun operations(kind: String): Map<String, String> = operations.filterKeys { it.second == kind }.mapKeys { it.key.first }
    fun put(id: String, record: ExportRecord) {
        schedule { database.records().put(StoredDownload(id, gson.toJson(record))); records[id] = record }
    }
    fun remove(id: String) { schedule { database.records().remove(id); records.remove(id) } }
    fun operation(id: String, kind: String, payload: String?) {
        if (payload == null) operations.remove(id to kind) else operations[id to kind] = payload
        schedule {
            if (payload == null) database.records().removeOperation(id, kind)
            else database.records().put(PendingOperation(id, kind, payload))
        }
    }
    private fun schedule(block: () -> Unit) {
        writer.execute { writes.enqueue(block); health.value = writes.failure?.javaClass?.simpleName; revisions.value += 1 }
    }
    suspend fun flush() {
        ready.await()
        val done = CompletableDeferred<Unit>()
        writer.execute {
            writes.drain()
            revisions.value += 1
            health.value = writes.failure?.javaClass?.simpleName
            writes.failure?.let { done.completeExceptionally(it) } ?: done.complete(Unit)
        }
        done.await()
    }
}
