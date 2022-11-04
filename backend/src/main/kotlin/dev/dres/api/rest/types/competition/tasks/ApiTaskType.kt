package dev.dres.api.rest.types.competition.tasks

import dev.dres.api.rest.types.competition.tasks.options.*
import dev.dres.data.model.template.task.TaskType

/**
 * The RESTful API equivalent of a [TaskType].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApiTaskType(
    val name: String,
    val duration: Long,
    val targetOption: ApiTargetOption,
    val hintOptions: List<ApiComponentOption>,
    val submissionOptions: List<ApiSubmissionOption>,
    val taskOptions: List<ApiTaskOption>,
    val scoreOption: ApiScoreOption,
    val configuration: Map<String,String>
)