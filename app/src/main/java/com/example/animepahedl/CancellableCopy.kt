package com.example.animepahedl

import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.coroutineContext

/** Closing descriptors interrupts a provider blocked in read/write when deletion or timeout cancels it. */
@OptIn(kotlinx.coroutines.InternalCoroutinesApi::class)
internal suspend fun copyCancellable(input: InputStream, output: OutputStream, onBytes: (Long) -> Unit): Long {
    val cancellation = coroutineContext[Job]?.invokeOnCompletion(onCancelling = true, invokeImmediately = true) {
        if (it != null) { runCatching { input.close() }; runCatching { output.close() } }
    }
    try {
        val buffer = ByteArray(256 * 1024)
        var copied = 0L
        while (true) {
            coroutineContext.ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            output.write(buffer, 0, count)
            copied += count
            onBytes(copied)
        }
        output.flush()
        return copied
    } catch (e: Exception) { coroutineContext.ensureActive(); throw e }
    finally { cancellation?.dispose() }
}
