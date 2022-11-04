package dev.dres.data.model.run

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.submissions.BatchedSubmission
import dev.dres.data.model.submissions.Submission
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

typealias TaskId  = String

/**
 * Represents a [Task], i.e., a concrete instance of a [TaskTemplate], as executed by DRES.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class Task(entity: Entity) : PersistentEntity(entity) {
    companion object : XdNaturalEntityType<Task>()

    /** The [TaskId] of this [Task]. */
    var taskId: TaskId
        get() = this.id
        set(value) { this.id = value }

    /** The [RunType] of this [Evaluation]. */
    var type by xdLink1(RunType)

    /** Timestamp of when this [Evaluation] started. */
    var started by xdRequiredLongProp()

    /** Timestamp of when this [Evaluation] ended. */
    var ended by xdNullableLongProp()

    /** The [TaskTemplate] this [Task] is an instance of. */
    var description by xdLink1(TaskTemplate)

    /** The [Evaluation] this [Task] belongs to. */
    var evaluation by xdParent<Task,Evaluation>(Evaluation::tasks)

    /** List of [Submission]s received by this [Task]. */
    val submissions by xdChildren0_N<Task,Submission>(Submission::task)

    /** List of [BatchedSubmission]s received by this [Task]. */
    val batched by xdChildren0_N<Task, BatchedSubmission>(BatchedSubmission::task)
}