package com.example.animepahedl

/** Identities survive overlapping failures without retaining UI retry closures. */
internal sealed interface OperationId {
    data class Search(val generation: Int) : OperationId
    data class Details(val session: String) : OperationId
    data class Sources(val session: String, val generation: Int) : OperationId
    data class Preparation(val session: String) : OperationId
    data class Download(val id: String) : OperationId
}

internal data class OperationFailure(val message: String, val url: String? = null,
    val browserUrl: String? = null, val verificationNeeded: Boolean = false)

/** Pure policy; elapsed time is supplied by the caller, never wall-clock time. */
internal class SearchVerificationGate {
    var generation = 0; private set
    private var editedAt = 0L
    private var nonempty = false
    private var submitted = false
    private var pending = false
    private var suppressed = false
    fun edit(query: String, now: Long): Int {
        generation++
        editedAt = now
        nonempty = query.isNotBlank()
        submitted = false
        pending = false
        suppressed = false
        return generation
    }
    fun submit() { submitted = true }
    fun challenge(generation: Int) { if (generation == this.generation && nonempty) pending = true }
    fun invalidate() { pending = false }
    fun suppress() { suppressed = true; pending = false }
    fun shouldPresent(now: Long, explore: Boolean, foreground: Boolean): Boolean =
        pending && !suppressed && nonempty && explore && foreground && (submitted || now - editedAt >= 1200)
    fun presented() { suppress() }
}
