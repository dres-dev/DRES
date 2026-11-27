package dev.dres.run

/** A point-in-time view of one viewer's task readiness and WebSocket liveness. */
data class ViewerStatus(
    val ready: Boolean,
    val presence: ViewerPresence?
)
