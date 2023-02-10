package dev.dres.api.rest.types.evaluation

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.api.rest.types.collection.ApiMediaItem
import dev.dres.data.model.run.Task
import dev.dres.data.model.submissions.*
import dev.dres.data.model.template.interfaces.EvaluationTemplate
import kotlinx.dnq.query.addAll

/**
 * The RESTful API equivalent for the type of [ApiAnswerSet].
 *
 * @see ApiAnswerSet
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApiAnswerSet(
    var status: ApiVerdictStatus,
    val answers: List<ApiAnswer>
) : AnswerSet {
    override val task: Task
        @JsonIgnore
        get() = TODO("Not yet implemented")
    override val submission: Submission
        @JsonIgnore
        get() = TODO("Not yet implemented")

    override fun answers(): Sequence<Answer> = answers.asSequence()
    override fun status(): VerdictStatus = VerdictStatus.fromApi(this.status)

    override fun status(status: VerdictStatus) {
        this.status = status.toApi()
    }

    override fun toDb(): DbAnswerSet {
        return DbAnswerSet.new {
            this.status = this@ApiAnswerSet.status.toDb()
            this.answers.addAll(
                this@ApiAnswerSet.answers.map {
                    it.toDb()
                }
            )
        }
    }
}
