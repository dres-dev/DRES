package dev.dres.api.rest.types.evaluation.submission

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.MediaItemId
import dev.dres.data.model.submissions.DbAnswer
import dev.dres.data.model.submissions.DbAnswerType
import io.javalin.openapi.OpenApiIgnore
import io.javalin.openapi.OpenApiNullable
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.singleOrNull
import kotlinx.serialization.Serializable

/**
 * The RESTful API equivalent of an answer as provided by a client of the submission API endpoints.
 *
 * There is an inherent asymmetry between the answers received by DRES (unprocessed & validated) and those sent by DRES (processed and validated).

 * @author Luca Rossetto
 * @version 1.0.0
 */
@Serializable
data class ApiClientAnswer( //TODO add optional relevance score field
    /** The text that is part of this [ApiClientAnswer]. */
    @get:OpenApiNullable
    val text: String? = null,

    /** The [MediaItemId] associated with the [ApiClientAnswer]. Is usually added as contextual information by the receiving endpoint. */
    @JsonIgnore
    @get:OpenApiIgnore
    val mediaItemId: MediaItemId? = null,

    /** The name of the media item that is part of the answer. */
    @get:OpenApiNullable
    val mediaItemName: String?  = null,

    /** The name of the collection the media item belongs to. */
    @get:OpenApiNullable
    val mediaItemCollectionName: String? = null,

    /** For temporal [ApiClientAnswer]s: Start of the segment in question in milliseconds. */
    @get:OpenApiNullable
    val start: Long? = null,

    /** For temporal [ApiClientAnswer]s: End of the segment in question in milliseconds. */
    @get:OpenApiNullable
    val end: Long? = null,
) {



    /**
     * Creates a new [DbAnswer] for this [ApiAnswer]. Requires an ongoing transaction.
     *
     * @return [DbAnswer]
     */
    fun toNewDb(): DbAnswer = DbAnswer.new {
        this.type = this@ApiClientAnswer.tryDetermineType()
        this.item = this@ApiClientAnswer.getDbItem()
        this.text = this@ApiClientAnswer.text
        this.start = this@ApiClientAnswer.start
        this.end = this@ApiClientAnswer.end
    }

    @JsonIgnore
    @OpenApiIgnore
    fun getDbItem() : DbMediaItem? {
        return when {
            this@ApiClientAnswer.mediaItemId != null ->
                DbMediaItem.filter { it.id eq mediaItemId }.singleOrNull()
                    ?: throw IllegalArgumentException("Could not find media item with ID ${this@ApiClientAnswer.mediaItemId}.")
            this@ApiClientAnswer.mediaItemName != null && this@ApiClientAnswer.mediaItemCollectionName != null ->
                DbMediaItem.filter { (it.name eq mediaItemName) and (it.collection.name eq this@ApiClientAnswer.mediaItemCollectionName) }.singleOrNull()
                    ?: throw IllegalArgumentException("Could not find media item with name '${this@ApiClientAnswer.mediaItemName}' in collection '${this@ApiClientAnswer.mediaItemCollectionName}'.")
            this@ApiClientAnswer.mediaItemName != null -> DbMediaItem.filter { it.name eq mediaItemName}.singleOrNull()
                ?: throw IllegalArgumentException("Could not find media item with name '${this@ApiClientAnswer.mediaItemName}'. Maybe collection name is required.")
            else -> null
        }
    }

    /**
     * Tries to determine the type of [ApiAnswer].
     *
     * @return The [DbAnswerType] for this [ApiClientAnswer].
     */
    private fun tryDetermineType() = when {
        this.mediaItemName != null && this.start != null && this.end != null -> DbAnswerType.TEMPORAL
        this.mediaItemName != null  -> DbAnswerType.ITEM
        this.text != null  -> DbAnswerType.TEXT
        else -> throw IllegalArgumentException("Could not determine answer type for provided answer.")
    }
}
