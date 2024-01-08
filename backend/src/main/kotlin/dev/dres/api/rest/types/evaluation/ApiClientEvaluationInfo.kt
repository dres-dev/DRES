package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.ApiRunProperties
import dev.dres.run.InteractiveAsynchronousRunManager
import dev.dres.run.InteractiveSynchronousRunManager
import dev.dres.run.NonInteractiveRunManager
import dev.dres.run.RunManager
import kotlinx.serialization.Serializable

/**
 * Contains the basic and most importantly static information about a [RunManager] that is relevant to a participant.
 *
 */
@Serializable
data class ApiClientEvaluationInfo(
    val id: String,
    val name: String,
    val type: ApiEvaluationType,
    val status: ApiEvaluationStatus,
    val templateId: String,
    val templateDescription: String?,
    val teams: List<String>,
    val taskTemplates: List<ApiClientTaskTemplateInfo>,
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
        manager.template.id,
        manager.template.description,
        manager.template.teams.asSequence().map { team -> team.name ?: "" }.toList(),
        manager.template.tasks.map { task -> ApiClientTaskTemplateInfo(task) }.toList()
    )
}
