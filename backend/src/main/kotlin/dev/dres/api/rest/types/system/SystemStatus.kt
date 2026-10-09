package dev.dres.api.rest.types.system

import kotlinx.serialization.Serializable

/** Severity for a system-level status shown to administrators. */
@Serializable
enum class SystemStatusLevel {
    INFO,
    WARNING,
    ERROR
}

/** An actionable system-level message for administrators. */
@Serializable
data class SystemStatus(
    val code: String,
    val level: SystemStatusLevel,
    val message: String
)
