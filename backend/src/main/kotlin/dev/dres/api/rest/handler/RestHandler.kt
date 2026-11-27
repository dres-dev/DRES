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

    /**
     * Runs [doGet] and answers an error if it throws.
     *
     * An [ErrorStatusException] is a deliberate, client-facing failure and keeps its status and message.
     * Everything else is a bug: the details go to the log and the client is told nothing specific.
     */
    fun get(ctx: Context) {
        try {
            ctx.json(doGet(ctx))
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        } catch (e: Exception) {
            ServerError.respond(ctx, e)
        }
    }

    fun doGet(ctx: Context): T
}

interface PostRestHandler<T: Any> : RestHandler {

    /** @see GetRestHandler.get */
    fun post(ctx: Context) {
        try {
            ctx.json(doPost(ctx))
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        } catch (e: Exception) {
            ServerError.respond(ctx, e)
        }
    }

    fun doPost(ctx: Context): T

}

interface PatchRestHandler<T: Any> : RestHandler {

    /** @see GetRestHandler.get */
    fun patch(ctx: Context) {
        try {
            ctx.json(doPatch(ctx))
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        } catch (e: Exception) {
            ServerError.respond(ctx, e)
        }
    }

    fun doPatch(ctx: Context): T

}

interface DeleteRestHandler<T: Any> : RestHandler {

    /** @see GetRestHandler.get */
    fun delete(ctx: Context) {
        try {
            ctx.json(doDelete(ctx))
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        } catch (e: Exception) {
            ServerError.respond(ctx, e)
        }
    }

    fun doDelete(ctx: Context): T

}

interface AccessManagedRestHandler : RestHandler {

    val permittedRoles: Set<RouteRole>

}