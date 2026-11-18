package com.example.animepahedl

import androidx.media3.exoplayer.offline.Downloader

internal class MediaLinkExpired(cause: Exception) : IllegalStateException("Link expired", cause)

/** Keep fallible setup inside Media3's task, where exceptions fail one item.
 * createDownloader itself runs on the manager's handler and must not do that work.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class DeferredDownloader(
    private val create: () -> Downloader,
    private val removeCached: () -> Unit,
    private val refresh: (() -> Downloader)? = null,
    private val expired: (Exception) -> Boolean = { false },
) : Downloader {
    private val lock = Any()
    private var delegate: Downloader? = null
    private var cancelled = false
    private var refreshed = false

    override fun download(progressListener: Downloader.ProgressListener?) {
        try {
            currentOrCreate(create).download(progressListener)
        } catch (failure: Exception) {
            val renew = refresh ?: throw failure
            if (!expired(failure)) throw failure
            if (refreshed) throw MediaLinkExpired(failure)
            refreshed = true
            synchronized(lock) { delegate = null }
            try { currentOrCreate(renew).download(progressListener) }
            catch (next: Exception) {
                if (expired(next)) throw MediaLinkExpired(next)
                throw next
            }
        }
    }

    private fun currentOrCreate(factory: () -> Downloader): Downloader {
        val existing = synchronized(lock) {
            if (cancelled) throw InterruptedException()
            delegate
        }
        return existing ?: factory().also { created ->
            synchronized(lock) {
                if (cancelled) {
                    created.cancel()
                    throw InterruptedException()
                }
                delegate = created
            }
        }
    }

    override fun cancel() {
        val current = synchronized(lock) { cancelled = true; delegate }
        current?.cancel()
    }

    // Removal must work even with missing credentials, metadata, or folder permission.
    override fun remove() = removeCached()
}
