package dres.data.model.competition.interfaces

import dres.data.model.competition.TaskGroup

/**
 * Basic description of a [Task].
 *
 * @author Ralph Gassser
 * @version 1.0
 */
interface TaskDescription {

    /** The [TaskType] of the [Task] */
    val taskGroup: TaskGroup
}