package dev.dres.api.rest.types.collection

import dev.dres.data.model.media.CollectionId
import dev.dres.data.model.media.DbMediaCollection
import kotlinx.dnq.query.size

/**
 * The RESTful API equivalent for [DbMediaCollection].
 *
 * @see DbMediaCollection
 * @author Ralph Gasser
 * @version 1.0
 */
data class ApiMediaCollection(

    val id: CollectionId? = null, // or we create a ApiCreateMediaCollection dto ?
    val name: String,
    val description: String? = null,
    val basePath: String? = null,
    val itemCount: Int = 0

    ) {
    companion object {
        /**
         * Generates a [ApiMediaCollection] from a [DbMediaCollection] and returns it.
         *
         * @param collection The [DbMediaCollection] to convert.
         */
        fun fromMediaCollection(collection: DbMediaCollection) = ApiMediaCollection(collection.id, collection.name, collection.description, collection.path, collection.items.size())
    }
}
