package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.submissions.VerdictStatus

/**
 * The RESTful API equivalent for the type of a [DbVerdictStatus]
 *
 * @see ApiAnswerSet
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiVerdictStatus(private val status: VerdictStatus.Status) : VerdictStatus {
    CORRECT(VerdictStatus.Status.CORRECT),
    WRONG(VerdictStatus.Status.WRONG),
    INDETERMINATE(VerdictStatus.Status.INDETERMINATE),
    UNDECIDABLE(VerdictStatus.Status.INDETERMINATE);

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

    override fun eq(status: VerdictStatus.Status): Boolean = status == this.status
}