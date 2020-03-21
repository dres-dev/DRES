package dres.api.rest.handler

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