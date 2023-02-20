package dev.dres.api.rest.types.evaluation

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.run.Task
import dev.dres.data.model.run.TaskId
import dev.dres.data.model.submissions.*
import kotlinx.dnq.query.addAll
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.first

/**
 * The RESTful API equivalent for the type of [ApiAnswerSet].
 *
 * @see ApiAnswerSet
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApiAnswerSet(
    var status: ApiVerdictStatus,
    override val taskId: TaskId,
    val answers: List<ApiAnswer>
) : AnswerSet {

    @JsonIgnore
    override lateinit var submission: ApiSubmission
    internal set

    override fun task(): Task {
        TODO("Not yet implemented")
    }

    override fun answers(): Sequence<Answer> = answers.asSequence()
    override fun status(): VerdictStatus = VerdictStatus.fromApi(this.status)

    override fun status(status: VerdictStatus) {
        this.status = status.toApi()
    }

    override fun toDb(): DbAnswerSet {
        return DbAnswerSet.new {
            this.status = this@ApiAnswerSet.status.toDb()
            this.task = DbTask.filter { taskId eq this@ApiAnswerSet.taskId }.first()
            this.answers.addAll(
                this@ApiAnswerSet.answers.map {
                    it.toDb()
                }
            )
        }
    }
}
