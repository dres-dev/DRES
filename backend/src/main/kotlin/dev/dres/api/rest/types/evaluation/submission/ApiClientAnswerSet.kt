package dev.dres.api.rest.types.evaluation.submission

import dev.dres.data.model.run.DbTask
import dev.dres.data.model.submissions.AnswerSetId
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbVerdictStatus
import kotlinx.dnq.query.addAll
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.first
import java.util.*

/**
 * The RESTful API equivalent of an answer set as provided by a client of the submission API endpoints.
 *
 * There is an inherent asymmetry between the answers sets received by DRES (unprocessed & validated) and those sent by DRES (processed and validated).
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
data class ApiClientAnswerSet(var taskId: String? = null, val taskName: String? = null, val answers: List<ApiClientAnswer>) {

    /** The [AnswerSetId] of this [ApiClientAnswerSet]. Typically generated by the receiving endpoint. */
    var answerSetId: AnswerSetId = UUID.randomUUID().toString()

    /**
     * Creates a new [DbAnswerSet] for this [ApiClientAnswerSet].
     *
     * Requires an ongoing transaction.
     *
     * @return [DbAnswerSet]
     */
    fun toNewDb(): DbAnswerSet = DbAnswerSet.new {
        this.id = this@ApiClientAnswerSet.answerSetId
        this.status = DbVerdictStatus.INDETERMINATE
        this.task = DbTask.filter { it.id eq this@ApiClientAnswerSet.taskId }.first()
        this.answers.addAll(
            this@ApiClientAnswerSet.answers.map { it.toNewDb() }
        )
    }
}
