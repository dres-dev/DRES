package dev.dres.api.rest.handler.preview

import dev.dres.api.rest.RestApi
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.collection.AbstractCollectionHandler
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.Config
import dev.dres.data.model.media.CollectionId
import dev.dres.data.model.media.MediaCollection
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.MediaType
import dev.dres.utilities.FFmpegUtil
import dev.dres.utilities.extensions.sendFile
import dev.dres.utilities.extensions.streamFile
import io.javalin.http.Context
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * An abstract [GetRestHandler] used to access preview images.
 *
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
abstract class AbstractPreviewHandler(protected val store: TransientEntityStore, config: Config) : GetRestHandler<Any>, AccessManagedRestHandler {
    companion object {
        /** Placeholder for when image is missing. */
        private val MISSING_IMAGE = this::class.java.getResourceAsStream("/img/missing.png").use { it!!.readAllBytes() }

        /** Placeholder for when image is waiting to be loaded. */
        private val WAITING_IMAGE = this::class.java.getResourceAsStream("/img/loading.png").use { it!!.readAllBytes() }
    }

    /** All [AbstractCollectionHandler]s require [ApiRole.VIEWER]. */
    override val permittedRoles = setOf(ApiRole.VIEWER)

    /** All [AbstractPreviewHandler]s are part of the v1 API. */
    override val apiVersion = "v1"

    /** The [Path] to the pre-calculated previews.*/
    private val cacheLocation = Paths.get(config.cachePath + "/previews")

    init {
        Files.createDirectories(this.cacheLocation)
    }

    /**
     * Handles a request for a preview based on an [CollectionId] and a [MediaItem]'s name. Fetching of the [MediaItem] takes
     * place in a transaction context. However, the (potentially) long running media processing is executed outside.
     *
     * @param collectionId [CollectionId] of the [MediaCollection].
     * @param itemName Name of the [MediaItem]
     * @param time The exact timepoint of the [MediaItem] in ms. Only works for [MediaType.VIDEO].
     * @param ctx The request [Context]
     */
    protected fun handlePreviewRequest(collectionId: CollectionId, itemName: String, time: Long?, ctx: Context) {
        val item = this.store.transactional(true) {
            MediaItem.query((MediaItem::name eq itemName) and (MediaItem::collection.matches(MediaCollection::id eq collectionId))).firstOrNull()
                ?: throw ErrorStatusException(404, "Media item $itemName (collection = $collectionId) not found!", ctx)
        }
        handlePreviewRequest(item, time, ctx)
    }

    /**
    * Handles a request for a preview based on an [MediaItem] and an optional timepoint.
    *
    * @param item The [MediaItem]
    * @param time The exact timepoint of the [MediaItem] in ms. Only works for [MediaType.VIDEO].
    * @param ctx The request [Context]
    */
    protected fun handlePreviewRequest(item: MediaItem, time: Long?, ctx: Context) {

        val basePath = Paths.get(item.collection.path)
        if (item.type == MediaType.IMAGE) {
            //TODO scale down image if too large
            ctx.header("Cache-Control", "max-age=31622400")
            ctx.streamFile(basePath.resolve(item.location))
            return
        } else if (item.type  == MediaType.VIDEO) {
            /* Prepare cache directory for item. */
            val cacheDir = this.cacheLocation.resolve("${item.collection}/${item.name}")
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir)
            }

            /* check timestamp. */
            if (time == null)  throw ErrorStatusException(400, "Timestamp unspecified or invalid.", ctx)

            val imgPath = cacheDir.resolve("${time}.jpg")
            if (Files.exists(imgPath)) { //if file is available, send contents immediately
                ctx.header("Cache-Control", "max-age=31622400")
                ctx.sendFile(imgPath)
            } else {
                val future = FFmpegUtil.executeFFmpegAsync(basePath.resolve(item.location), time, imgPath)
                val waitTime = if (RestApi.readyThreadCount > 500) {
                    3L
                } else {
                    1L
                }

                try {
                    val path = future.get(waitTime, TimeUnit.SECONDS) ?: throw FileNotFoundException()
                    ctx.sendFile(path.toFile())
                } catch (e: TimeoutException) {
                    ctx.status(408)
                    ctx.header("Cache-Control", "max-age=30")
                    ctx.contentType("image/png")
                    ctx.result(WAITING_IMAGE)
                } catch (t: Throwable) {
                    ctx.status(429)
                    ctx.header("Cache-Control", "max-age=600")
                    ctx.contentType("image/png")
                    ctx.result(MISSING_IMAGE)
                }
            }
        }
    }
}





