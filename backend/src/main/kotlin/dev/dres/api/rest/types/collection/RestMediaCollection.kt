package dev.dres.api.rest.types.collection

import dev.dres.data.model.media.CollectionId
import dev.dres.data.model.media.MediaCollection
import dev.dres.data.model.template.task.TaskTemplate

/**
 * The RESTful API equivalent for [MediaCollection].
 *
 * @see MediaCollection
 * @author Ralph Gasser
 * @version 1.0
 */
data class RestMediaCollection(val id: CollectionId, val name: String, val description: String? = null, val basePath: String? = null) {
    companion object {
        /**
         * Generates a [ApiMediaItem] from a [TaskTemplate] and returns it.
         *
         * @param task The [TaskTemplate] to convert.
         */
        fun fromMediaCollection(item: MediaCollection) = RestMediaCollection(item.id, item.name, item.description, item.path)
    }
}