package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.ApiRunProperties
import dev.dres.run.InteractiveAsynchronousRunManager
import dev.dres.run.InteractiveSynchronousRunManager
import dev.dres.run.NonInteractiveRunManager
import dev.dres.run.RunManager

/**
 * Contains the basic and most importantly static information about a [RunManager].
 *
 * Since this information usually doesn't change in the course of a run, it allows for local caching and other optimizations.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
data class ApiEvaluationInfo(
    val id: String,
    val name: String,
    val type: ApiEvaluationType,
    val status: ApiEvaluationStatus,
    val properties: ApiRunProperties,
    val templateId: String,
    val templateDescription: String?,
    val teams: List<ApiTeamInfo>,
    val taskTemplates: List<ApiTaskTemplateInfo>,
) {
    constructor(manager: RunManager): this(
        manager.id,
        manager.name,
        when(manager) {
            is InteractiveSynchronousRunManager -> ApiEvaluationType.SYNCHRONOUS
            is InteractiveAsynchronousRunManager -> ApiEvaluationType.ASYNCHRONOUS
            is NonInteractiveRunManager -> ApiEvaluationType.NON_INTERACTIVE
            else -> throw IllegalStateException("Incompatible type of run manager.")
        },
        manager.status.toApi(),
        manager.runProperties,
        manager.template.id,
        manager.template.description,
        manager.template.teams.asSequence().map { team -> ApiTeamInfo(team) }.toList(),
        manager.template.tasks.map { task -> ApiTaskTemplateInfo(task) }.toList()
    )
}
