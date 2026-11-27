package dev.dres.api.rest.types

import dev.dres.api.rest.handler.users.SessionToken
import dev.dres.utilities.extensions.realIP
import dev.dres.utilities.extensions.sessionToken
import io.javalin.http.Context
import io.javalin.websocket.WsContext

data class ViewerInfo(val sessionToken: SessionToken, val host: String) {
    constructor(ctx: Context) : this(ctx.sessionToken()!!, ctx.realIP())
    constructor(ctx: WsContext) : this(ctx.sessionToken()!!, ctx.realIP())
}
