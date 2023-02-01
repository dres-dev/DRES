package dev.dres.api.rest.handler.collection

import dev.dres.api.rest.handler.PostRestHandler
import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.types.status.SuccessStatus
import dev.dres.data.model.media.MediaCollection
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.MediaType
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import java.util.*

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class AddMediaItemHandler(store: TransientEntityStore) : AbstractCollectionHandler(store), PostRestHandler<SuccessStatus> {

    override val route: String = "mediaitem"

    @OpenApi(
        summary = "Adds a media item to the specified media collection.",
        path = "/api/v1/mediaItem",
        methods = [HttpMethod.POST],
        requestBody = OpenApiRequestBody([OpenApiContent(ApiMediaItem::class)]),
        tags = ["Collection"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context): SuccessStatus {

        /* Parse media item and perform sanity checks */
        val mediaItem = try {
            ctx.bodyAsClass(ApiMediaItem::class.java)
        } catch (e: BadRequestResponse) {
            throw ErrorStatusException(400, "Invalid parameters. This is a programmers error!", ctx)
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, e.message ?: "Invalid parameters. This is a programmers error!", ctx)
        }

        /* Try to persist media item. */
        val collectionId = mediaItem.collectionId
        return this.store.transactional {
            val collection = MediaCollection.query(MediaCollection::id eq collectionId).firstOrNull()
                ?: throw ErrorStatusException(400, "Invalid parameters, collection with ID $collectionId does not exist.", ctx)
            if (collection.items.filter { it.name eq mediaItem.name }.isNotEmpty) {
                throw ErrorStatusException(400, "Media item with name '${mediaItem.name}' already exists in collection ${collection.name}.", ctx)
            }

            val item = MediaItem.new {
                this.id = UUID.randomUUID().toString()
                this.type = mediaItem.type.toMediaType()
                this.name = mediaItem.name
                this.location = mediaItem.location
                this.fps = mediaItem.fps
                this.durationMs = mediaItem.durationMs
            }
            collection.items.add(item)

            SuccessStatus("Media item added successfully.")
        }
    }
}