package dres.data.model.competition.interfaces

import dres.data.model.competition.TaskType

/**
 * Basic description of a [Task].
 *
 * @author Ralph Gassser
 * @version 1.0
 */
interface TaskDescription {

    /** The [TaskType] of the [Task] */
    val taskType: TaskType
}