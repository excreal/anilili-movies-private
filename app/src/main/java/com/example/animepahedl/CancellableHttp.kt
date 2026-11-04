package com.example.animepahedl

import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import okhttp3.Call
import okhttp3.Response
import kotlin.coroutines.coroutineContext

/** Keep cancellation connected until the entire response body has been consumed. */
@OptIn(kotlinx.coroutines.InternalCoroutinesApi::class)
internal suspend fun <T> Call.consumeCancellable(block: (Response) -> T): T {
    coroutineContext.ensureActive()
    val cancellation = coroutineContext[Job]?.invokeOnCompletion(onCancelling = true, invokeImmediately = true) {
        if (it != null) cancel()
    }
    try {
        return execute().use { response ->
            coroutineContext.ensureActive()
            block(response).also { coroutineContext.ensureActive() }
        }
    } catch (e: Exception) {
        coroutineContext.ensureActive()
        throw e
    } finally { cancellation?.dispose() }
}
