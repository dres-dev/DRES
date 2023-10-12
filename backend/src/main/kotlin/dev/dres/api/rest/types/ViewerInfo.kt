package dev.dres.api.rest.types

import dev.dres.api.rest.handler.users.SessionToken

data class ViewerInfo(val sessionToken: SessionToken, val host: String)
