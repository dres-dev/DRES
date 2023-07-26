package dev.dres.api.rest.types.evaluation.submission

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.data.model.run.TaskId
import dev.dres.data.model.submissions.*

/**
 * The RESTful API equivalent for the type of an answer set as submitted by the DRES endpoint.
 *
 * There is an inherent asymmetry between the answers sets received by DRES (unprocessed & validated) and those sent by DRES (processed and validated).
 *
 * @see ApiAnswerSet
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApiAnswerSet(
    override val id: AnswerSetId,
    val status: ApiVerdictStatus,
    override val taskId: TaskId,
    val answers: List<ApiAnswer>
) : AnswerSet {

    @get:JsonIgnore
    override lateinit var submission: Submission
        internal set

    override fun status(): VerdictStatus = when(this.status) {
        ApiVerdictStatus.CORRECT -> VerdictStatus.CORRECT
        ApiVerdictStatus.WRONG -> VerdictStatus.WRONG
        ApiVerdictStatus.INDETERMINATE -> VerdictStatus.INDETERMINATE
        ApiVerdictStatus.UNDECIDABLE -> VerdictStatus.UNDECIDABLE
    }

    override fun answers(): Sequence<Answer> = answers.asSequence()
}
