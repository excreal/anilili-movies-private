package com.example.animepahedl

/** Single-writer journal. A failed head stays in place until it can commit. */
internal class OrderedWrites {
    private val pending = java.util.ArrayDeque<() -> Unit>()
    var failure: Exception? = null
        private set
    fun enqueue(write: () -> Unit) { pending.addLast(write); drain() }
    fun drain() {
        while (pending.isNotEmpty()) {
            try { pending.peekFirst()!!.invoke(); pending.removeFirst() }
            catch (e: Exception) { failure = e; return }
        }
        failure = null
    }
}
