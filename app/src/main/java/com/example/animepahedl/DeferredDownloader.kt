package com.example.animepahedl

import androidx.media3.exoplayer.offline.Downloader

/** Keep fallible setup inside Media3's task, where exceptions fail one item.
 * createDownloader itself runs on the manager's handler and must not do that work.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class DeferredDownloader(
    private val create: () -> Downloader,
    private val removeCached: () -> Unit,
) : Downloader {
    private val lock = Any()
    private var delegate: Downloader? = null
    private var cancelled = false

    override fun download(progressListener: Downloader.ProgressListener?) {
        val existing = synchronized(lock) {
            if (cancelled) throw InterruptedException()
            delegate
        }
        val downloader = existing ?: create().also { created ->
            synchronized(lock) {
                if (cancelled) {
                    created.cancel()
                    throw InterruptedException()
                }
                delegate = created
            }
        }
        downloader.download(progressListener)
    }

    override fun cancel() {
        val current = synchronized(lock) { cancelled = true; delegate }
        current?.cancel()
    }

    // Removal must work even with missing credentials, metadata, or folder permission.
    override fun remove() = removeCached()
}
