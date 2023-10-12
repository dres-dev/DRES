package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.RestApi
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.types.users.ApiRole
import io.javalin.security.RouteRole

abstract class AbstractExternalItemHandler : AccessManagedRestHandler {

    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN)
    override val apiVersion: String = RestApi.LATEST_API_VERSION
}
