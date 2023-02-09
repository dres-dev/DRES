package dev.dres.run.score

import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.template.team.TeamId

/**
 *
 */
data class TaskContext(val taskId: EvaluationId, val teamIds: Collection<TeamId>, val taskStartTime: Long?, val taskDuration: Long?, val taskEndTime: Long? = null)
