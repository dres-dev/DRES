package dev.dres.data.model.submissions

import dev.dres.api.rest.types.evaluation.submission.ApiAnswerSet
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.run.TaskId
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence

/**
 * A [DbAnswerSet] as submitted by a evaluation participant. This is a portion of a proposed 'solution' for a [DbTask].
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
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

    /** The [DbAnswer]s that belong to this [DbAnswerSet]. */
    val answers by xdChildren1_N<DbAnswerSet, DbAnswer>(DbAnswer::answerSet)

    /** Implementation of the [AnswerSet] interface: The ID of the [DbTask] this [DbAnswerSet] is associated with. */
    override val taskId: TaskId
        get() = this.task.id

    /**
     * Implementation of the [AnswerSet] interface: Returns the [VerdictStatus] representation of this [DbAnswerSet]'s [DbVerdictStatus].
     *
     * @return [VerdictStatus] of this [DbAnswerSet].
     */
    override fun status(): VerdictStatus = VerdictStatus.valueOf(this.status.description)

    /**
     * Implementation of the [AnswerSet] interface: Returns a [Sequence] of [Answer]s.
     *
     * @return [Sequence] of [Answer]s.
     */
    override fun answers(): Sequence<Answer> = this.answers.asSequence()

    /**
     * Converts this [DbAnswerSet] to a RESTful API representation [ApiAnswerSet].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @param blind True, if a "blind" [ApiAnswerSet] should be generated.
     * @return [ApiAnswerSet]
     */
    fun toApi(blind: Boolean = false): ApiAnswerSet = ApiAnswerSet(
        id = this.id,
        status = this.status.toApi(),
        taskId = this.task.id,
        answers = this.answers.asSequence().map { it.toApi(blind) }.toList()
    )
}
