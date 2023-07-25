package dev.dres.api.rest.types.template.tasks

import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.media.CollectionId
import dev.dres.data.model.template.TemplateId

/**
 * The RESTful API equivalent for [DbTaskTemplate].
 *
 * @see DbTaskTemplate
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
data class ApiTaskTemplate(
    val id: TemplateId? = null,
    val name: String,
    val taskGroup: String,
    val taskType: String,
    val duration: Long,
    val collectionId: CollectionId,
    val targets: List<ApiTarget>,
    val hints: List<ApiHint>,
    val comment: String = ""
)
