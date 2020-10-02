package dev.dres.api.rest.types.collection

import dev.dres.data.dbo.DAO
import dev.dres.data.model.UID

import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.data.model.competition.TaskDescription
import dev.dres.utilities.extensions.UID

/**
 * The RESTful API equivalent for [dres.data.model.basics.media.MediaCollection].
 *
 * @see dres.data.model.basics.media.MediaCollection
 * @author Ralph Gasser
 * @version 1.0
 */
data class RestMediaCollection(val id: String = UID.EMPTY.string, val name: String, val description: String? = null, val basePath: String? = null) {
    companion object {
        /**
         * Generates a [RestMediaItem] from a [TaskDescription] and returns it.
         *
         * @param task The [TaskDescription] to convert.
         */
        fun fromMediaCollection(item: MediaCollection) = RestMediaCollection(item.id.string, item.name, item.description, item.basePath)
    }

    /**
     * Converts this [RestMediaCollection] to the corresponding [MediaCollection] and returns it,
     * by looking it up in the database.
     *
     * @param mediaCollections The [DAO] to perform lookups.
     */
    fun toMediaCollection(mediaCollections: DAO<MediaCollection>) = mediaCollections[this.id.UID()]
}