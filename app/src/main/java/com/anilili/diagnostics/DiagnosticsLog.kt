package com.anilili.diagnostics

/** Keep the original player's diagnostics local to the host. */
internal object DiagnosticsLog {
    fun event(message: String) { android.util.Log.d("ReferencePlayer", message) }
    fun event(category: String, name: String, attributes: Map<String, Any?> = emptyMap(), message: String? = null) {
        com.example.animepahedl.DebugTrace.event("reference.$category.$name", *attributes.entries.map { it.key to it.value }.toTypedArray())
        if (message != null) event(message)
    }
    fun failure(category: String, name: String, message: String? = null, attributes: Map<String, Any?> = emptyMap(), throwable: Throwable? = null) {
        event(category, name, attributes, message)
        if (throwable != null) this.throwable(message ?: name, throwable)
    }
    fun throwable(message: String, error: Throwable) { android.util.Log.w("ReferencePlayer", message, error) }
}
internal object CrashReporter {
    fun logNonFatal(message: String, error: Throwable) { DiagnosticsLog.throwable(message, error) }
}
