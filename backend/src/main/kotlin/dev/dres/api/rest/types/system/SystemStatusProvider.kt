package dev.dres.api.rest.types.system

/** Supplies current system-level status messages. Implementations should be fast and side-effect free. */
fun interface SystemStatusProvider {
    fun statuses(): List<SystemStatus>
}
