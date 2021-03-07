package dev.dres.run.score

import dev.dres.data.model.competition.TeamId

data class TaskContext(val teamIds: Collection<TeamId>, val taskStartTime: Long?, val taskDuration: Long?, val taskEndTime: Long? = null)
