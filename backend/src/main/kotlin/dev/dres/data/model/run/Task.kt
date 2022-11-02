package dev.dres.data.model.run

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.competition.task.TaskDescription
import dev.dres.data.model.submissions.Submission
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

typealias TaskId  = String

/**
 * Represents a [Task], i.e., a concrete instance of a [TaskDescription], as executed by DRES.
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

    /** The [TaskDescription] this [Task] is an instance of. */
    var description by xdLink1(TaskDescription)

    /** The [Evaluation] this [Task] belongs to. */
    var evaluation by xdParent<Task,Evaluation>(Evaluation::tasks)

    /** List of [Submission]s received by this [Task]. */
    val submissions by xdChildren0_N<Task,Submission>(Submission::task)
}