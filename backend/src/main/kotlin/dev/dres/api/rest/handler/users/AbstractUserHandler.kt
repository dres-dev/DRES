package dev.dres.api.rest.handler.users

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.RestHandler
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.users.ApiUser
import dev.dres.data.model.admin.DbUser
import dev.dres.mgmt.admin.UserManager
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context

typealias SessionToken = String

/**
 * An abstract [RestHandler] to manage [DbUser]s
 *
 * @author Loris Sauter
 * @version 2.0.0
 */
abstract class AbstractUserHandler: RestHandler, AccessManagedRestHandler {
    /** All [AbstractUserHandler]s are part of the v1 API. */
    override val apiVersion = "v2"

    /** Convenience method to extract [DbUser] from current session. */
    protected fun userFromSession(ctx: Context): ApiUser {
        return UserManager.get(id = AccessManager.userIdForSession(ctx.sessionToken())!!)
            ?: throw ErrorStatusException(404, "User could not be found!", ctx)
    }

    /** Convenience method to extract [DbUser] from [Context] (userId parameter). */
    protected fun userFromContext(ctx: Context): ApiUser {
        val id = ctx.pathParamMap()["userId"] ?: throw ErrorStatusException(404, "Parameter 'userId' is missing!'", ctx)
        return UserManager.get(id = id) ?: throw ErrorStatusException(404, "User ($id) not found!", ctx)
    }
}