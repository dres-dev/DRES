package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.submissions.DbAnswerType

/**
 * The RESTful API equivalent for the type of a [DbAnswerType]
 *
 * @see ApiAnswerSet
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiAnswerType {
    ITEM, TEMPORAL, TEXT;

    /**
     * Converts this [ApiAnswerType] to a [DbAnswerType] representation. Requires an ongoing transaction.
     *
     * @return [DbAnswerType]
     */
    fun toDb(): DbAnswerType = when(this) {
        ITEM -> DbAnswerType.ITEM
        TEMPORAL -> DbAnswerType.TEMPORAL
        TEXT -> DbAnswerType.TEXT
    }
}