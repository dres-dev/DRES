package dev.dres.api.rest.types.audit

import dev.dres.run.audit.AuditLogEntry

/**
 * A collection of [AuditLogEntry]s with pagination information
 */
data class AuditLogPage(
        val index: Int = 0,
        val entries: List<AuditLogEntry>,
        val timestamp: Long = System.currentTimeMillis(),
        val oldest: Long
)

