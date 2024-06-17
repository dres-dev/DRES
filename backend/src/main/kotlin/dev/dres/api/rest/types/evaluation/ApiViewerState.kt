package dev.dres.api.rest.types.evaluation

import dev.dres.api.rest.types.evaluation.scores.ApiScore
import dev.dres.api.rest.types.evaluation.scores.ApiTeamGroupValue
import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.api.rest.types.template.team.ApiTeamGroup
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.template.task.TaskTemplateId
import kotlinx.serialization.Serializable

/**
 * A DTO to transfer the initial state to the viewer.
 * Contains all relevant information for the viewer to initiate itself.
 */
@Serializable
data class ApiViewerState(
    /** The [EvaluationId] of the evaluation this state represents */
    val evaluationId: EvaluationId,
    /** The name of the evaluation this state represents */
    val evaluationName: String,
    /** The [ApiTeamInfo]s related to this evaluation */
    val teamInfos: List<ApiTeamInfo>,
    /** The [ApiTeamGroup]s of this evaluation */
    val teamGroups: List<ApiTeamGroup>,
    /** The [ApiTeamGroupValue]s associated with the [ApiTeamGroup]s */
    val teamGroupValue: List<ApiTeamGroupValue>,

    /** The [ApiScore]s of the active task */
    val taskScores: List<ApiScore>,
    /** The [ApiScore]s, grouped for the entire evaluation */
    val evaluationScores: Map<String, List<ApiScore>>, // ApiScoreOverview had taskGroup in it

    /** The [TaskId] of the current task. Could also only be selected task so far */
    val activeTaskId: TaskId,
    /** The [TaskTemplateId] of the current task. */
    val activeTaskTemplateId: TaskTemplateId,
    /** The time elapsed so far */
    val timeElapsed: Long,
    /** The time remaining. Null means the task is perpetually running */
    val timeRemaining: Long?, // Prepare perpetual task
    /** The [ApiTaskStatus] of the currently active task */
    val activeTaskStatus: ApiTaskStatus,
    /** The [ApiSubmission]s related to the currently active task */
    val submissionsOverview: List<ApiSubmission>
)
