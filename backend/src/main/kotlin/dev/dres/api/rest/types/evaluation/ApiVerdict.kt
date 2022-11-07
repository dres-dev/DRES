package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.collection.ApiMediaItem

/**
 * The RESTful API equivalent for the type of a [ApiVerdict].
 *
 * @see ApiVerdict
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApiVerdict(
    val type: ApiVerdictType,
    val status: ApiVerdictStatus,
    val item: ApiMediaItem?,
    val text: String?,
    val start: Long?,
    val end: Long?
)