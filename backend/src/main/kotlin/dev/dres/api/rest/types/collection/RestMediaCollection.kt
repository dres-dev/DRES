package dev.dres.api.rest.types.collection

import dev.dres.data.model.basics.media.CollectionId
import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.data.model.competition.TaskDescription
import dev.dres.utilities.extensions.UID

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
         * Generates a [RestMediaItem] from a [TaskDescription] and returns it.
         *
         * @param task The [TaskDescription] to convert.
         */
        fun fromMediaCollection(item: MediaCollection) = RestMediaCollection(item.id, item.name, item.description, item.path)
    }
}