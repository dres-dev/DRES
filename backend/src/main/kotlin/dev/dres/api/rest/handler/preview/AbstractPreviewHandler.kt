package dev.dres.api.rest.handler.preview

import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.handler.AccessManagedRestHandler
import dev.dres.api.rest.handler.GetRestHandler
import dev.dres.api.rest.handler.collection.AbstractCollectionHandler
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.data.model.Config
import dev.dres.data.model.media.CollectionId
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.DbMediaType
import dev.dres.utilities.FFmpegUtil
import dev.dres.utilities.extensions.sendFile
import dev.dres.utilities.extensions.streamFile
import io.javalin.http.Context
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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
    override val apiVersion = "v2"

    /** The [Path] to the pre-calculated previews.*/
    private val cacheLocation = Paths.get(config.cachePath + "/previews")

    init {
        Files.createDirectories(this.cacheLocation)
    }

    /**
     * Handles a request for a preview based on an [CollectionId] and a [DbMediaItem]'s name. Fetching of the [DbMediaItem] takes
     * place in a transaction context. However, the (potentially) long running media processing is executed outside.
     *
     * @param collectionId [CollectionId] of the [DbMediaCollection].
     * @param itemName Name of the [DbMediaItem]
     * @param time The exact timepoint of the [DbMediaItem] in ms. Only works for [DbMediaType.VIDEO].
     * @param ctx The request [Context]
     */
    protected fun handlePreviewRequest(collectionId: CollectionId, itemName: String, time: Long?, ctx: Context) {
        val item = this.store.transactional(true) {
            DbMediaItem.query((DbMediaItem::name eq itemName) and (DbMediaItem::collection.matches(DbMediaCollection::id eq collectionId))).firstOrNull()
                ?: throw ErrorStatusException(404, "Media item $itemName (collection = $collectionId) not found!", ctx)
        }
        handlePreviewRequest(item, time, ctx)
    }

    /**
    * Handles a request for a preview based on an [DbMediaItem] and an optional timepoint.
    *
    * @param item The [DbMediaItem]
    * @param time The exact timepoint of the [DbMediaItem] in ms. Only works for [DbMediaType.VIDEO].
    * @param ctx The request [Context]
    */
    protected fun handlePreviewRequest(item: DbMediaItem, time: Long?, ctx: Context) {

        val basePath = Paths.get(item.collection.path)
        if (item.type == DbMediaType.IMAGE) {
            //TODO scale down image if too large
            ctx.header("Cache-Control", "max-age=31622400")
            ctx.streamFile(basePath.resolve(item.location))
            return
        } else if (item.type == DbMediaType.VIDEO) {

            /* Prepare cache directory for item. */
            val cacheDir = cacheLocation.resolve("${item.collection}/${item.name}")
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir)
            }

            /* check timestamp. */
            if (time == null) {
                throw ErrorStatusException(400, "Timestamp unspecified or invalid.", ctx)
            }


            val imgPath = cacheDir.resolve("${time}.jpg")

            if (Files.exists(imgPath)) { //if file is available, send contents immediately
                ctx.header("Cache-Control", "max-age=31622400")
                ctx.sendFile(imgPath.toFile())
            } else { //if not, schedule and return error

                FFmpegUtil.extractFrame(basePath.resolve(item.location), time, imgPath)
                ctx.status(408)
                ctx.header("Cache-Control", "max-age=30")

            }
        }
    }
}





