package dev.dres.api.rest.handler.users

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.RestHandler
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.admin.User
import dev.dres.mgmt.admin.UserManager
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context

typealias SessionToken = String

/**
 * An abstract [RestHandler] to manage [User]s
 *
 * @author Loris Sauter
 * @version 2.0.0
 */
abstract class AbstractUserHandler: RestHandler, AccessManagedRestHandler {
    /** All [AbstractUserHandler]s are part of the v1 API. */
    override val apiVersion = "v2"

    /** Convenience method to extract [User] from current session. */
    protected fun userFromSession(ctx: Context): User {
        return UserManager.get(id = AccessManager.userIdForSession(ctx.sessionToken())!!)
            ?: throw ErrorStatusException(404, "User could not be found!", ctx)
    }

    /** Convenience method to extract [User] from [Context] (userId parameter). */
    protected fun userFromContext(ctx: Context): User {
        val id = ctx.pathParamMap()["userId"] ?: throw ErrorStatusException(404, "Parameter 'userId' is missing!'", ctx)
        return UserManager.get(id = id) ?: throw ErrorStatusException(404, "User ($id) not found!", ctx)
    }
}