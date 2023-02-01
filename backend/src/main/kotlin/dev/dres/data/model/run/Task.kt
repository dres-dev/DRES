package dev.dres.data.model.run

import dev.dres.api.rest.types.evaluation.ApiTask
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.Verdict
import dev.dres.data.model.template.team.Team
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.asSequence

typealias EvaluationId = String

/**
 * Represents a [Task], i.e., a concrete instance of a [TaskTemplate], as executed by DRES.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class Task(entity: Entity) : PersistentEntity(entity) {
    companion object : XdNaturalEntityType<Task>()

    /** The [EvaluationId] of this [Task]. */
    var taskId: EvaluationId
        get() = this.id
        set(value) { this.id = value }

    /** Timestamp of when this [Evaluation] started. */
    var started by xdRequiredLongProp()

    /** Timestamp of when this [Evaluation] ended. */
    var ended by xdNullableLongProp()

    /** The [TaskTemplate] this [Task] is an instance of. */
    var template by xdLink1(TaskTemplate)

    /** Link to a [Team] this [Task] was created for. Can be NULL!*/
    var team by xdLink0_1(Team)

    /** The [Evaluation] this [Task] belongs to. */
    var evaluation: Evaluation by xdParent<Task,Evaluation>(Evaluation::tasks)

    /** List of [Submission]s received by this [Task]. */
    val submissions by xdChildren0_N<Task,Verdict>(Verdict::task)

    /**
     * Converts this [Task] to a RESTful API representation [ApiTask].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiTask]
     */
    fun toApi(): ApiTask = ApiTask(
        taskId = this.taskId,
        templateId = this.template.id,
        started = this.started,
        ended = this.ended,
        submissions = this.submissions.asSequence().map { it.toApi() }.toList()
    )
}