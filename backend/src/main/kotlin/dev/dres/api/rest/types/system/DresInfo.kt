package dev.dres.api.rest.types.system

import dev.dres.DRES

/**
 * Information about the DRES instance.
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
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
    val availableSeverThreads: Int? = null
)