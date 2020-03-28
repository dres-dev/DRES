package dres.api.rest.handler

import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import java.lang.Exception

interface RestHandler {

    val route: String

}

interface GetRestHandler<T: Any> : RestHandler {

    fun get(ctx: Context) {
        try {
            ctx.json(doGet(ctx))
        } catch (e: ErrorStatusException) {
            ctx.status(e.statusCode).json(e.errorStatus)
        } catch (e: Exception) {
            ctx.status(500).json(ErrorStatus(e.message ?: ""))
        }
    }

    fun doGet(ctx: Context): T

}

interface PostRestHandler<T: Any> : RestHandler {

    fun post(ctx: Context) {
        try {
            ctx.json(doPost(ctx))
        } catch (e: ErrorStatusException) {
            ctx.status(e.statusCode).json(e.errorStatus)
        } catch (e: Exception) {
            ctx.status(500).json(ErrorStatus(e.message ?: ""))
        }
    }

    fun doPost(ctx: Context): T

}

interface PatchRestHandler<T: Any> : RestHandler {

    fun patch(ctx: Context) {
        try {
            ctx.json(doPatch(ctx))
        } catch (e: ErrorStatusException) {
            ctx.status(e.statusCode).json(e.errorStatus)
        } catch (e: Exception) {
            ctx.status(500).json(ErrorStatus(e.message ?: ""))
        }
    }

    fun doPatch(ctx: Context): T

}

interface DeleteRestHandler<T: Any> : RestHandler {

    fun delete(ctx: Context) {
        try {
            ctx.json(doDelete(ctx))
        } catch (e: ErrorStatusException) {
            ctx.status(e.statusCode).json(e.errorStatus)
        } catch (e: Exception) {
            ctx.status(500).json(ErrorStatus(e.message ?: ""))
        }
    }

    fun doDelete(ctx: Context): T

}

interface AccessManagedRestHandler : RestHandler {

    val permittedRoles: Set<Role>

}