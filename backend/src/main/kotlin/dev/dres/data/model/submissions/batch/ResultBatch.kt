package dev.dres.data.model.submissions.batch

import dev.dres.data.model.template.TeamId
import dev.dres.data.model.run.interfaces.TaskId

/**
 *
 * @author Luca Rossetto
 * @version 1.0.0
 */
interface ResultBatch<T: BaseResultBatchElement> {
    val task: TaskId
    val teamId: TeamId
    val name: String
    val results: List<T>
}