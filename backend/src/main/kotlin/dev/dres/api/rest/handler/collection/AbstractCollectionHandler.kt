package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.RestHandler
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.media.MediaCollection
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
abstract class AbstractCollectionHandler(protected val store: TransientEntityStore) : RestHandler, AccessManagedRestHandler {

    /** All [AbstractCollectionHandler]s require [ApiRole.ADMIN]. */
    override val permittedRoles: Set<RouteRole> = setOf(ApiRole.ADMIN)

    /** All [AbstractCollectionHandler]s are part of the v1 API. */
    override val apiVersion = "v2"

    /** Convenience method to extract [MediaCollection] from [Context]. */
    protected fun collectionFromContext(ctx: Context): MediaCollection {
        val id = ctx.pathParamMap()["collectionId"] ?: throw ErrorStatusException(404, "Parameter 'collectionId' is missing!'", ctx)
        return MediaCollection.query(MediaCollection::id eq id).firstOrNull() ?:  throw ErrorStatusException(404, "Collection with ID $id not found.'", ctx)
    }
}