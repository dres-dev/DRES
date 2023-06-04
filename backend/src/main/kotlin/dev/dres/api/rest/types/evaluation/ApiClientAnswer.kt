package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.submissions.AnswerType

data class ApiClientAnswer(
    val text: String?,
    val itemName: String?,
    val itemCollectionName: String?,
    val start: Long?, // ms
    val end: Long? // ms
) {
    fun type() : AnswerType? = when {
        this.text != null -> AnswerType.TEXT
        this.itemName == null -> null
        this.start != null && this.end != null -> AnswerType.TEMPORAL
        else -> AnswerType.ITEM
    }
}
