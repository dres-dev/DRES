package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.InteractiveSynchronousEvaluation
import dev.dres.data.model.run.RunProperties
import dev.dres.run.RunManager

/**
 * Contains the basic and most importantly static information about a [InteractiveSynchronousEvaluation] and the
 * associated [RunManager]. Since this information usually doesn't change in the course of a run,
 * it allows for local caching and other optimizations.
 *
 * @author Ralph Gasser
 * @version 1.0.2
 */
data class ApiEvaluationInfo(
        val id: String,
        val name: String,
        val templateId: String,
        val templateDescription: String?,
        val type: ApiRunType,
        val properties: RunProperties,
        val teams: List<TeamInfo>,
        val tasks: List<TaskInfo>,
)