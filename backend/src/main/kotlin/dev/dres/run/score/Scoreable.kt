package dev.dres.run.score

import dev.dres.data.model.run.Task
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.template.team.TeamId

/**
 * A [Scoreable] is subject of a [TaskScorer].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface Scoreable {
    /** The [TaskId] of the [Task] masked by this [Scoreable]. */
    val taskId: TaskId

    /** The [TeamId]s of teams that work on the task identified by this [Scoreable]. */
    val teams: List<TeamId>

    /** Duration of when the [Task] in seconds. */
    val duration: Long

    /** Timestamp of when the [Task] identified by this [Scoreable] was started. */
    val started: Long?

    /** Timestamp of when the [Task] identified by this [Scoreable] ended. */
    val ended: Long?
}
