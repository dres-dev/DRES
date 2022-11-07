package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.submissions.VerdictStatus

/**
 * The RESTful API equivalent for the type of a [VerdictStatus]
 *
 * @see ApiVerdict
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiVerdictStatus(val status: VerdictStatus) {
    CORRECT(VerdictStatus.CORRECT),
    WRONG(VerdictStatus.WRONG),
    INDETERMINATE(VerdictStatus.INDETERMINATE),
    UNDECIDABLE(VerdictStatus.UNDECIDABLE)
}