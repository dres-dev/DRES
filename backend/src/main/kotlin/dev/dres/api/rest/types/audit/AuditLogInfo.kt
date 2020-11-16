package dev.dres.api.rest.types.audit

import dev.dres.run.audit.AuditLogEntry

data class AuditLogInfo(
        val timestamp: Long = System.currentTimeMillis(),
        val size: Int,
        val latest: Long
)