package dev.dres.api.rest.handler.preview

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.data.model.media.MediaItem
import dev.dres.utilities.extensions.errorResponse
import dev.dres.utilities.extensions.streamFile
import io.javalin.http.Context
import io.javalin.openapi.HttpMethod
import io.javalin.openapi.OpenApi
import io.javalin.openapi.OpenApiParam
import io.javalin.openapi.OpenApiResponse
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.eq
import kotlinx.dnq.query.firstOrNull
import kotlinx.dnq.query.query
import java.nio.file.Paths

/**
 * An [GetRestHandler] used to access the files that belong to a specific [MediaItem].
 *
 * @author Luca Rossetto
 * @version 2.0.0
 */
class GetMediaHandler(private val store: TransientEntityStore) : GetRestHandler<Any>, AccessManagedRestHandler {
    override val route: String = "media/{collection}/{item}"

    /** All [GetMediaHandler] can be used by [ApiRole.VIEWER], [ApiRole.PARTICIPANT] and [ApiRole.ADMIN]. */
    override val permittedRoles = setOf(ApiRole.VIEWER, ApiRole.PARTICIPANT, ApiRole.ADMIN)

    /** [GetMediaHandler] is part of the v1 API. */
    override val apiVersion = "v1"

    //not used
    override fun doGet(ctx: Context): Any = ""

    @OpenApi(summary = "Returns a collection item",
            path = "/api/v1/media/{itemId}",
            pathParams = [
                OpenApiParam("itemId", String::class, "The media item ID.")
            ],
            tags = ["Media"],
            responses = [OpenApiResponse("200"), OpenApiResponse("401"), OpenApiResponse("400"), OpenApiResponse("404")],
            ignore = true,
        methods = [HttpMethod.GET]
    )
    override fun get(ctx: Context) {
        /* Extract parameter. */
        val itemId = ctx.pathParamMap()["itemId"]
        if (itemId == null) {
            ctx.errorResponse(400, "Missing parameters!")
            return
        }

        /* Lookup item by ID. */
        val item = this.store.transactional(true) {
            MediaItem.query(MediaItem::id eq itemId).firstOrNull()
        }
        if (item == null) {
            ctx.errorResponse(404, "item with name $itemId found")
            return
        }

        try{
            ctx.streamFile( Paths.get(item.collection.path).resolve(item.location))
        } catch (e: org.eclipse.jetty.io.EofException) {
            //is triggered by a client abruptly stopping playback, can be safely ignored
        }
    }
}