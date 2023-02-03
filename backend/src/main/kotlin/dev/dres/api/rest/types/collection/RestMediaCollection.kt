package dev.dres.api.rest.types.collection

import dev.dres.data.model.media.CollectionId
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.data.model.template.task.DbTaskTemplate

/**
 * The RESTful API equivalent for [DbMediaCollection].
 *
 * @see DbMediaCollection
 * @author Ralph Gasser
 * @version 1.0
 */
data class RestMediaCollection(val id: CollectionId, val name: String, val description: String? = null, val basePath: String? = null) {
    companion object {
        /**
         * Generates a [ApiMediaItem] from a [DbTaskTemplate] and returns it.
         *
         * @param task The [DbTaskTemplate] to convert.
         */
        fun fromMediaCollection(item: DbMediaCollection) = RestMediaCollection(item.id, item.name, item.description, item.path)
    }
}