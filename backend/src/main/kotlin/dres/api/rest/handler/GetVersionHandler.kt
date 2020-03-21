package dres.api.rest.handler

import dres.api.rest.RestApiRole
import io.javalin.core.security.Role
import io.javalin.http.Context

class GetVersionHandler : GetRestHandler, AccessManagedRestHandler {
    override fun get(ctx: Context) {
        ctx.result("0.1")
    }

    override val permittedRoles: Set<Role> = setOf(RestApiRole.ADMIN)

    override val route: String = "version"


}