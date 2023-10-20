package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.DbEvaluationType
/**
 *
 */
enum class ApiEvaluationType {
    SYNCHRONOUS, ASYNCHRONOUS, NON_INTERACTIVE;

    /**
     * Converts this [ApiEvaluationType] to a [DbEvaluationType] representation. Requires an ongoing transaction.
     *
     * @return [DbEvaluationType]
     */
    fun toDb(): DbEvaluationType = when(this) {
        SYNCHRONOUS -> DbEvaluationType.INTERACTIVE_SYNCHRONOUS
        ASYNCHRONOUS -> DbEvaluationType.INTERACTIVE_ASYNCHRONOUS
        NON_INTERACTIVE -> DbEvaluationType.NON_INTERACTIVE
    }
}