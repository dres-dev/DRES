package dev.dres.api.rest.types.competition.tasks

import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.media.CollectionId
import dev.dres.data.model.template.TemplateId

/**
 * The RESTful API equivalent for [TaskTemplate].
 *
 * @see TaskTemplate
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
class ApiTaskTemplate(
    val id: TemplateId? = null,
    val name: String,
    val taskGroup: String,
    val taskType: String,
    val duration: Long,
    val collectionId: CollectionId,
    val targets: List<ApiTarget>,
    val hints: List<ApiHint>
)