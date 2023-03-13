package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.ApiAnswerSet
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.run.Task
import dev.dres.data.model.run.TaskId
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence

/**
 * A [DbVerdictStatus] as submitted by a competition participant. Makes a statement about a [DbTask].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @version 2.0.0
 */
class DbAnswerSet(entity: Entity) : PersistentEntity(entity), AnswerSet {
    companion object : XdNaturalEntityType<DbAnswerSet>()

    /** The [DbSubmission] this [DbAnswerSet] belongs to. */
    override var submission: DbSubmission by xdParent<DbAnswerSet,DbSubmission>(DbSubmission::answerSets)

    /** The [DbVerdictStatus] of this [DbAnswerSet]. */
    var status: DbVerdictStatus by xdLink1(DbVerdictStatus)

    /** The [DbTask] this [DbAnswerSet] belongs to. */
    var task: DbTask by xdLink1<DbAnswerSet, DbTask>(DbTask::answerSets)

    override fun task(): Task = task
    override val taskId: TaskId
        get() = task.taskId

    val answers by xdChildren1_N<DbAnswerSet, DbAnswer>(DbAnswer::answerSet)

    override fun answers(): Sequence<Answer> = answers.asSequence()
    override fun status(): VerdictStatus = VerdictStatus.fromDb(status)

    override fun status(status: VerdictStatus) {
        this.status = status.toDb()
    }

    /**
     * Converts this [DbVerdictStatus] to a RESTful API representation [ApiAnswerSet].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiAnswerSet]
     */
    fun toApi(): ApiAnswerSet = ApiAnswerSet(
        id = this.id,
        status = this.status.toApi(),
        taskId = this.taskId,
        answers = this.answers.asSequence().map { it.toApi() }.toList()
    )
}
