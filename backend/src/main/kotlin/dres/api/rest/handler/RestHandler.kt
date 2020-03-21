package dres.api.rest.handler

import io.javalin.core.security.Role
import io.javalin.http.Context

interface RestHandler {

    val route: String

}

interface GetRestHandler : RestHandler {

    fun get(ctx: Context)

}

interface PostRestHandler : RestHandler {

    fun post(ctx: Context)

}

interface PatchRestHandler : RestHandler {

    fun patch(ctx: Context)

}

interface DeleteRestHandler : RestHandler {

    fun delete(ctx: Context)

}

interface AccessManagedRestHandler : RestHandler {

    val permittedRoles: Set<Role>

}