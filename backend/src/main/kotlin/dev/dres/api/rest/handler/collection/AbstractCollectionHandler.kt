package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.RestApi
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.RestHandler
import dev.dres.api.rest.types.collection.ApiMediaCollection
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.mgmt.MediaCollectionManager
import io.javalin.http.Context
import io.javalin.security.RouteRole
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
abstract class AbstractCollectionHandler() : RestHandler, AccessManagedRestHandler {

    /** All [AbstractCollectionHandler]s require [ApiRole.ADMIN]. */
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN)

    /** All [AbstractCollectionHandler]s are part of the v2 API. */
    override val apiVersion = RestApi.LATEST_API_VERSION

    protected fun collectionId(ctx: Context) = ctx.pathParamMap()["collectionId"] ?: throw ErrorStatusException(404, "Parameter 'collectionId' is missing!'", ctx)
}
