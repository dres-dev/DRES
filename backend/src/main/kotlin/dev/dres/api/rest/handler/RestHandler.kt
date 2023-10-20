package dev.dres.api.rest.handler

import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.utilities.extensions.errorResponse
import io.javalin.security.RouteRole
import io.javalin.http.Context

interface RestHandler {

    val route: String
    val apiVersion: String

}

interface GetRestHandler<T: Any> : RestHandler {

    fun get(ctx: Context) {
        try {
            ctx.json(doGet(ctx))
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        } catch (e: Exception) {
            ctx.errorResponse(500, e.message ?: "")
        }
    }

    fun doGet(ctx: Context): T
}

interface PostRestHandler<T: Any> : RestHandler {

    fun post(ctx: Context) {
        try {
            ctx.json(doPost(ctx))
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        } catch (e: Exception) {
            ctx.errorResponse(500,e.message ?: "")
        }
    }

    fun doPost(ctx: Context): T

}

interface PatchRestHandler<T: Any> : RestHandler {

    fun patch(ctx: Context) {
        try {
            ctx.json(doPatch(ctx))
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        } catch (e: Exception) {
            ctx.errorResponse(500, e.message ?: "")
        }
    }

    fun doPatch(ctx: Context): T

}

interface DeleteRestHandler<T: Any> : RestHandler {

    fun delete(ctx: Context) {
        try {
            ctx.json(doDelete(ctx))
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        } catch (e: Exception) {
            ctx.errorResponse(500, e.message ?: "")
        }
    }

    fun doDelete(ctx: Context): T

}

interface AccessManagedRestHandler : RestHandler {

    val permittedRoles: Set<RouteRole>

}