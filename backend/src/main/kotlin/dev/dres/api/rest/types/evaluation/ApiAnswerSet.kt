package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.data.model.run.Task
import dev.dres.data.model.submissions.Answer
import dev.dres.data.model.submissions.AnswerSet
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.data.model.template.interfaces.EvaluationTemplate

/**
 * The RESTful API equivalent for the type of [ApiAnswerSet].
 *
 * @see ApiAnswerSet
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApiAnswerSet(
    val status: ApiVerdictStatus,
    val answers: List<ApiAnswer>
) : AnswerSet {
    override val task: Task
        get() = TODO("Not yet implemented")
    override val submission: Submission
        get() = TODO("Not yet implemented")

    override fun answers(): Sequence<Answer> = answers.asSequence()
    override fun status(): VerdictStatus {
        TODO("Not yet implemented")
    }

    override fun status(status: VerdictStatus) {
        TODO("Not yet implemented")
    }
}
