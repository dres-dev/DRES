package dev.dres.api.rest.types

import dev.dres.api.rest.handler.users.SessionToken
import dev.dres.utilities.extensions.realIP
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context

data class ViewerInfo(val sessionToken: SessionToken, val host: String) {
    constructor(ctx: Context) : this(ctx.sessionToken()!!, ctx.realIP())
}
