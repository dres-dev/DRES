package dev.dres.api.rest.types.audit

import dev.dres.data.model.audit.DbAuditLogSource

/**
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiAuditLogSource {
    REST, CLI, INTERNAL;

    /**
     * Converts this [ApiAuditLogSource] to a RESTful API representation [DbAuditLogSource].
     *
     * @return [DbAuditLogSource]
     */
    fun toDb(): DbAuditLogSource = when(this) {
        REST -> DbAuditLogSource.REST
        CLI -> DbAuditLogSource.CLI
        INTERNAL -> DbAuditLogSource.INTERNAL
    }
}