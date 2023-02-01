package dev.dres.api.rest.handler.audit

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.RestHandler
import dev.dres.api.rest.handler.collection.AbstractCollectionHandler
import io.javalin.security.RouteRole
import jetbrains.exodus.database.TransientEntityStore

/**
 *
 * @author Loris Sauter
 * @version 2.0.0
 */
abstract class AbstractAuditLogHandler(protected val store: TransientEntityStore) : RestHandler, AccessManagedRestHandler {
    /** All [AbstractCollectionHandler]s require [ApiRole.ADMIN]. */
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN)

    /** All [AbstractCollectionHandler]s are part of the v1 API. */
    override val apiVersion = "v2"
}