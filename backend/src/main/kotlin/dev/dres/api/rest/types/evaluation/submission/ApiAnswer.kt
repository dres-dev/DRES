package dev.dres.api.rest.types.evaluation.submission

import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.data.model.submissions.Answer
import dev.dres.data.model.submissions.AnswerType

/**
 * The RESTful API equivalent for the type of answer as submitted by the DRES endpoint.
 *
 * There is an inherent asymmetry between the answers received by DRES (unprocessed & validated) and those sent by DRES (processed and validated).
 *
 * @see ApiAnswerSet
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApiAnswer(
    /** The [ApiAnswerType] of this [ApiAnswer]. */
    val type: ApiAnswerType,

    /** For [ApiAnswer]s of type [ApiAnswerType.ITEM] or [ApiAnswerType.TEMPORAL]: The [ApiMediaItem] that is part of the [ApiAnswer]. */
    override val item: ApiMediaItem?,

    /** For [ApiAnswer]s of type [ApiAnswerType.TEXT]: The text that is part of this [ApiAnswer]. */
    override val text: String?,

    /** For [ApiAnswer]s of type [ApiAnswerType.TEMPORAL]: Start of the segment in question in milliseconds that is part of this [ApiAnswer]. */
    override val start: Long? = null,

    /** For [ApiAnswer]s of type [ApiAnswerType.TEMPORAL]: Start of the segment in question in milliseconds that is part of this [ApiAnswer]. */
    override val end: Long? = null
) : Answer {
    override fun type(): AnswerType = when(type) {
        ApiAnswerType.TEMPORAL -> AnswerType.TEMPORAL
        ApiAnswerType.ITEM -> AnswerType.ITEM
        ApiAnswerType.TEXT -> AnswerType.TEXT
    }
}
