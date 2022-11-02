package dev.dres.data.model.run

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.competition.task.TaskDescription
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
    companion object : XdNaturalEntityType<Competition>()

    /** The [TaskId] of this [Task]. */
    var taskId: TaskId
        get() = this.id
        set(value) { this.id = value }

    /** The [RunType] of this [Competition]. */
    var type by xdLink1(RunType)

    /** Timestamp of when this [Competition] started. */
    var started by xdRequiredLongProp()

    /** Timestamp of when this [Competition] ended. */
    var ended by xdNullableLongProp()

    /** The [TaskDescription] this [Task] is an instance of. */
    var description by xdLink1(TaskDescription)

    /** The [Competition] this [Task] belongs to. */
    var competition by xdParent<Task,Competition>(Competition::tasks)

}