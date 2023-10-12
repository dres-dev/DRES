package dev.dres.api.rest.handler.download

import dev.dres.api.rest.RestApi
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.types.users.ApiRole
import io.javalin.security.RouteRole
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [AccessManagedRestHandler] implementation that provides certain data structures as downloadable files.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractDownloadHandler : AccessManagedRestHandler {

    /** The version of the API this [AbstractDownloadHandler] belongs to. */
    override val apiVersion = RestApi.LATEST_API_VERSION

    /** The roles permitted to access the [AbstractDownloadHandler]. */
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN, ApiRole.PARTICIPANT)
}
