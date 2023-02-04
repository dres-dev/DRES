package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.collection.ApiMediaItem

/**
 * The RESTful API equivalent for the type of a [ApiAnswerSet].
 *
 * @see ApiAnswerSet
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApiAnswerSet(
    val status: ApiVerdictStatus,
    val answers: List<ApiAnswer>
)
