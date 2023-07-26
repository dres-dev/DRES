package dev.dres.api.rest.types.evaluation.submission

import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.MediaItemId
import dev.dres.data.model.submissions.DbAnswer
import dev.dres.data.model.submissions.DbAnswerType
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.singleOrNull

/**
 * The RESTful API equivalent of an answer as provided by a client of the submission API endpoints.
 *
 * There is an inherent asymmetry between the answers received by DRES (unprocessed & validated) and those sent by DRES (processed and validated).

 * @author Luca Rossetto
 * @version 1.0.0
 */
data class ApiClientAnswer(
    /** The text that is part of this [ApiClientAnswer]. */
    val text: String? = null,

    /** The name of the media item that is part of the answer. */
    val itemName: String?  = null,

    /** The name of the collection the media item belongs to. */
    val itemCollectionName: String? = null,

    /** For temporal [ApiClientAnswer]s: Start of the segment in question in milliseconds. */
    val start: Long? = null,

    /** For temporal [ApiClientAnswer]s: End of the segment in question in milliseconds. */
    val end: Long? = null,
) {

    /** The [MediaItemId] associated with the [ApiClientAnswer]. Is usually added as contextual information by the receiving endpoint. */
    val mediaItemId: MediaItemId? = null

    /**
     * Creates a new [DbAnswer] for this [ApiAnswer]. Requires an ongoing transaction.
     *
     * @return [DbAnswer]
     */
    fun toNewDb(): DbAnswer = DbAnswer.new {
        this.type = this@ApiClientAnswer.tryDetermineType()
        this.item = when {
            this@ApiClientAnswer.mediaItemId != null ->
                DbMediaItem.filter { it.id eq mediaItemId }.singleOrNull()
                    ?: throw IllegalArgumentException("Could not find media item with ID ${this@ApiClientAnswer.mediaItemId}.")
            this@ApiClientAnswer.itemName != null && this@ApiClientAnswer.itemCollectionName != null ->
                DbMediaItem.filter { (it.name eq itemName) and (it.collection.name eq this@ApiClientAnswer.itemCollectionName) }.singleOrNull()
                    ?: throw IllegalArgumentException("Could not find media item with name '${this@ApiClientAnswer.itemName}' in collection '${this@ApiClientAnswer.itemCollectionName}'.")
            this@ApiClientAnswer.itemName != null -> DbMediaItem.filter { it.name eq itemName}.singleOrNull()
                    ?: throw IllegalArgumentException("Could not find media item with name '${this@ApiClientAnswer.itemName}'. Maybe collection name is required.")
            else -> null
        }
        this.text = this@ApiClientAnswer.text
        this.start = this@ApiClientAnswer.start
        this.end = this@ApiClientAnswer.end
    }

    /**
     * Tries to determine the type of [ApiAnswer].
     *
     * @return The [DbAnswerType] for this [ApiClientAnswer].
     */
    fun tryDetermineType() = when {
        this.itemName != null && this.start != null && this.end != null -> DbAnswerType.TEMPORAL
        this.itemName != null  -> DbAnswerType.ITEM
        this.text != null  -> DbAnswerType.TEXT
        else -> throw IllegalArgumentException("Could not determine answer type for provided answer.")
    }
}
