package dev.dres.data.model.submissions.batch

import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.run.interfaces.TaskId

/**
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
data class BaseResultBatch<T: BaseResultBatchElement>(
    override val task: TaskId,
    override val name: String,
    override val teamId: TeamId,
    override val results: List<T>
) : ResultBatch<T>