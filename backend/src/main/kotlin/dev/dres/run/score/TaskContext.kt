package dev.dres.run.score

import dev.dres.data.model.run.TaskId
import dev.dres.data.model.template.team.TeamId

/**
 *
 */
data class TaskContext(val taskId: TaskId, val teamIds: Collection<TeamId>, val taskStartTime: Long?, val taskDuration: Long?, val taskEndTime: Long? = null)
