package dev.dres.run

/** Current WebSocket presence for one viewer session. */
data class ViewerPresence(
    val host: String,
    val ready: Boolean,
    val connected: Boolean,
    val lastSeen: Long
)
