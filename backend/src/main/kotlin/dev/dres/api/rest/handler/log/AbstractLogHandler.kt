package dev.dres.api.rest.handler.log

import dev.dres.api.rest.RestApi
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.api.rest.types.users.ApiRole
import io.javalin.security.RouteRole

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
abstract class AbstractLogHandler: PostRestHandler<SuccessStatus>, AccessManagedRestHandler {
    /** All [AbstractLogHandler]s are part of the v1 API. */
    override val apiVersion = RestApi.LATEST_API_VERSION

    /** All [AbstractLogHandler]s require [ApiRole.ADMIN] or [ApiRole.PARTICIPANT]. */
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN, ApiRole.PARTICIPANT)
}
