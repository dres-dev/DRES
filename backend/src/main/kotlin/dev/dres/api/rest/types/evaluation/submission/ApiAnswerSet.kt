package dev.dres.api.rest.types.evaluation.submission

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
data class ApiAnswerSet(val id: AnswerSetId, val status: ApiVerdictStatus, val taskId: TaskId, val answers: List<ApiAnswer>)
