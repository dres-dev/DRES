package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.submissions.DbVerdictStatus

/**
 * The RESTful API equivalent for the type of a [DbVerdictStatus]
 *
 * @see ApiAnswerSet
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiVerdictStatus() {
    CORRECT, WRONG, INDETERMINATE, UNDECIDABLE;

    /**
     * Converts this [ApiVerdictStatus] to a [DbVerdictStatus] representation. Requires an ongoing transaction.
     *
     * @return [DbVerdictStatus]
     */
    fun toDb(): DbVerdictStatus = when(this) {
        CORRECT -> DbVerdictStatus.CORRECT
        WRONG -> DbVerdictStatus.WRONG
        INDETERMINATE -> DbVerdictStatus.INDETERMINATE
        UNDECIDABLE -> DbVerdictStatus.UNDECIDABLE
    }

}