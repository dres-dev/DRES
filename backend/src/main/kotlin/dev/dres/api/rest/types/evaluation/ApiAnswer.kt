package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.collection.ApiMediaItem

data class ApiAnswer(
    val type: ApiAnswerType,
    val item: ApiMediaItem?,
    val text: String?,
    val start: Long?,
    val end: Long?
    )
