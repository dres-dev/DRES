package dev.dres.api.rest.handler.system

import dev.dres.api.rest.types.system.SystemStatus
import dev.dres.api.rest.types.system.SystemStatusProvider
import java.util.concurrent.CopyOnWriteArrayList

/** Registry for optional system health checks that contribute admin-visible notices. */
object SystemStatusRegistry : SystemStatusProvider {
    const val FFMPEG_VIDEO_RENDER_FAILED = "FFMPEG_VIDEO_RENDER_FAILED"

    private val providers = CopyOnWriteArrayList<SystemStatusProvider>()

    init {
        providers.add(FfmpegSystemStatusProvider())
    }

    /** Adds a provider once; intended for subsystem initialization and tests. */
    fun register(provider: SystemStatusProvider) {
        if (providers.none { it === provider }) providers.add(provider)
    }

    fun unregister(provider: SystemStatusProvider) {
        providers.remove(provider)
    }

    fun publish(status: SystemStatus) {
        transientStatuses[status.code] = status
    }

    fun clear(code: String) {
        transientStatuses.remove(code)
    }

    private val transientStatuses = java.util.concurrent.ConcurrentHashMap<String, SystemStatus>()

    override fun statuses(): List<SystemStatus> {
        val providerStatuses = providers.flatMap { provider ->
            runCatching { provider.statuses() }
                .onFailure { System.err.println("WARNING: System status provider failed: ${it.message}") }
                .getOrDefault(emptyList())
        }
        return (providerStatuses + transientStatuses.values).distinctBy { it.code }.sortedBy { it.code }
    }
}
