package dev.dres.api.rest.handler.preview

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.collection.AbstractCollectionHandler
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.media.CollectionId
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.DbMediaType
import dev.dres.mgmt.cache.CacheManager
import dev.dres.utilities.CompletedFuture
import dev.dres.utilities.FailedFuture
import dev.dres.utilities.extensions.sendFile
import dev.dres.utilities.extensions.streamFile
import io.javalin.http.Context
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import java.nio.file.Files

/**
 * An abstract [GetRestHandler] used to access preview images.
 *
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
abstract class AbstractPreviewHandler(protected val store: TransientEntityStore, private val cache: CacheManager) : GetRestHandler<Any>, AccessManagedRestHandler {

    companion object {
        /** Placeholder for when image is waiting to be loaded. */
        private val WAITING_IMAGE = this::class.java.getResourceAsStream("/img/loading.png").use { it!!.readAllBytes() }
    }

    /** All [AbstractCollectionHandler]s require [ApiRole.VIEWER]. */
    override val permittedRoles = setOf(ApiRole.VIEWER)

    /** All [AbstractPreviewHandler]s are part of the v1 API. */
    override val apiVersion = "v2"

    /**
     * Handles a request for a preview based on an [CollectionId] and a [DbMediaItem]'s name. Fetching of the [DbMediaItem] takes
     * place in a transaction context. However, the (potentially) long-running media processing is executed outside.
     *
     * @param collectionId [CollectionId] of the [DbMediaCollection].
     * @param itemName Name of the [DbMediaItem]
     * @param time The exact timepoint of the [DbMediaItem] in ms. Only works for [DbMediaType.VIDEO].
     * @param ctx The request [Context]
     */
    protected fun handlePreviewImageRequest(collectionId: CollectionId, itemName: String, time: Long?, ctx: Context) {
        this.store.transactional(true) {
            val item = DbMediaItem.query((DbMediaItem::name eq itemName) and (DbMediaItem::collection.matches(DbMediaCollection::id eq collectionId))).firstOrNull()
                ?: throw ErrorStatusException(404, "Media item $itemName (collection = $collectionId) not found!", ctx)
            handlePreviewImageRequest(item, time, ctx)
        }
    }

    /**
    * Handles a request for a preview based on an [DbMediaItem] and an optional timepoint.
    *
    * @param item The [DbMediaItem]
    * @param time The exact timepoint of the [DbMediaItem] in ms. Only works for [DbMediaType.VIDEO].
    * @param ctx The request [Context]
    */
    protected fun handlePreviewImageRequest(item: DbMediaItem, time: Long?, ctx: Context) {
        when(val result = this@AbstractPreviewHandler.cache.asyncPreviewImage(item, time ?: 0)) {
            is FailedFuture -> throw ErrorStatusException(500, "Failed to load preview image.", ctx)
            is CompletedFuture -> {
                ctx.status(200)
                ctx.header("Cache-Control", "max-age=31622400")
                ctx.sendFile(result.get())
            }
            else -> {
                ctx.status(202)
                ctx.header("Cache-Control", "no-store")
                ctx.outputStream().write(WAITING_IMAGE)
            }
        }
    }

    /**
     * Handles a request for a preview video based on an [DbMediaItem] and a start and end timepoint.
     *
     * @param item The [DbMediaItem]
     * @param start The start timepoint of the [DbMediaItem] in ms. Only works for [DbMediaType.VIDEO].
     * @param end The end timepoint of the [DbMediaItem] in ms. Only works for [DbMediaType.VIDEO].
     * @param ctx The request [Context]
     */
    protected fun handlePreviewVideoRequest(item: DbMediaItem, start: Long, end: Long, ctx: Context) {
        if (item.type != DbMediaType.VIDEO) throw ErrorStatusException(400, "Selected media item ${item.mediaItemId} is not a video.", ctx)
        if (start <= end) throw ErrorStatusException(400, "Start timestamp must be greater than end timestamp.", ctx)
        when(val result = this@AbstractPreviewHandler.cache.asyncPreviewVideo(item, start, end)) {
            is FailedFuture -> throw ErrorStatusException(500, "Failed to load preview video.", ctx)
            is CompletedFuture -> {
                ctx.status(200)
                ctx.header("Cache-Control", "max-age=31622400")
                ctx.streamFile(result.get())
            }
            else -> {
                ctx.status(202)
                ctx.header("Cache-Control", "no-store")
            }
        }
    }
}





