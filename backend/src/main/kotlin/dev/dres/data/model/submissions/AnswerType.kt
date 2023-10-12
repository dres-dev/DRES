package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.submission.ApiAnswerType

enum class AnswerType {
    ITEM, TEMPORAL, TEXT;

    fun toDb(): DbAnswerType = when(this) {
        ITEM -> DbAnswerType.ITEM
        TEMPORAL -> DbAnswerType.TEMPORAL
        TEXT -> DbAnswerType.TEXT
    }

    fun toApi() : ApiAnswerType = when(this) {
        ITEM -> ApiAnswerType.ITEM
        TEMPORAL -> ApiAnswerType.TEMPORAL
        TEXT -> ApiAnswerType.TEXT
    }

    companion object {

        fun fromApi(apiAnswerType: ApiAnswerType) = when(apiAnswerType) {
            ApiAnswerType.ITEM -> ITEM
            ApiAnswerType.TEMPORAL -> TEMPORAL
            ApiAnswerType.TEXT -> TEXT
        }

        fun fromDb(dbAnswerType: DbAnswerType) = when(dbAnswerType) {
            DbAnswerType.ITEM -> ITEM
            DbAnswerType.TEMPORAL -> TEMPORAL
            DbAnswerType.TEXT -> TEXT
            else -> throw IllegalStateException("Unknown DbAnswerType $dbAnswerType")
        }

    }

}