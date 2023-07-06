package dev.dres.api.rest.types.template.tasks

import dev.dres.api.rest.types.template.tasks.options.*
import dev.dres.data.model.template.task.DbTaskType

/**
 * The RESTful API equivalent of a [DbTaskType].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class ApiTaskType(
    val name: String,
    val duration: Long,
    val targetOption: ApiTargetOption,
    val hintOptions: List<ApiHintOption>,
    val submissionOptions: List<ApiSubmissionOption>,
    val taskOptions: List<ApiTaskOption>,
    val scoreOption: ApiScoreOption,
    val configuration: Map<String,String>
)
