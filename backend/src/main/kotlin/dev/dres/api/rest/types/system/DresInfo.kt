package dev.dres.api.rest.types.system

import dev.dres.DRES
import kotlinx.serialization.Serializable

/**
 * Information about the DRES instance.
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
@Serializable
data class DresInfo(
    val version: String = DRES.VERSION,
    val startTime: Long,
    val uptime: Long,
    val os: String? = null,
    val jvm: String? = null,
    val args: String? = null,
    val cores: Int? = null,
    val freeMemory: Long? = null,
    val totalMemory: Long? = null,
    val load: Double? = null,
    val availableSeverThreads: Int? = null,
    /** Current system-level notices; populated only for administrator requests. */
    val systemStatuses: List<SystemStatus> = emptyList()
)