package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.InteractiveSynchronousEvaluation
import dev.dres.data.model.run.RunProperties
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.run.InteractiveAsynchronousRunManager
import dev.dres.run.InteractiveSynchronousRunManager
import dev.dres.run.NonInteractiveRunManager
import dev.dres.run.RunManager
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.sortedBy

/**
 * Contains the basic and most importantly static information about a [InteractiveSynchronousEvaluation] and the
 * associated [RunManager]. Since this information usually doesn't change in the course of a run,
 * it allows for local caching and other optimizations.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
data class ApiEvaluationInfo(
    val id: String,
    val name: String,
    val templateId: String,
    val templateDescription: String?,
    val type: ApiEvaluationType,
    val properties: RunProperties, // FIXME non-api type exposed via api
    val teams: List<ApiTeamInfo>,
    val tasks: List<ApiTaskTemplateInfo>,
) {
    constructor(manager: RunManager): this(
        manager.id,
        manager.name,
        manager.template.id,
        manager.template.name,
        when(manager) {
            is InteractiveSynchronousRunManager -> ApiEvaluationType.SYNCHRONOUS
            is InteractiveAsynchronousRunManager -> ApiEvaluationType.ASYNCHRONOUS
            is NonInteractiveRunManager -> ApiEvaluationType.NON_INTERACTIVE
            else -> throw IllegalStateException("Incompatible type of run manager.")
        },
        manager.runProperties,
        manager.template.teams.asSequence().map { team -> ApiTeamInfo(team) }.toList(),
        manager.template.tasks.sortedBy(DbTaskTemplate::idx).asSequence().map { task -> ApiTaskTemplateInfo(task) }.toList()
    )
}
