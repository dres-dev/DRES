package dev.dres.api.rest.types.template.tasks

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.api.rest.types.evaluation.ApiTaskTemplateInfo
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.media.CollectionId
import dev.dres.data.model.template.TemplateId
import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.template.task.TaskTemplateId
import io.javalin.openapi.OpenApiIgnore
import kotlinx.serialization.Serializable

/**
 * The RESTful API equivalent for [DbTaskTemplate].
 *
 * @see DbTaskTemplate
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
@Serializable
data class ApiTaskTemplate(
    val id: TaskTemplateId? = null,
    val name: String,
    val taskGroup: String,
    val taskType: String,
    val duration: Long,
    val collectionId: CollectionId,
    val targets: List<ApiTarget>,
    val hints: List<ApiHint>,
    val comment: String?
) : TaskTemplate {
    override fun textualDescription(): String = this.hints.filter { it.type == ApiHintType.TEXT }.maxByOrNull { it.start ?: 0 }?.description ?: name

    override val templateId: TaskTemplateId
        @JsonIgnore
        @OpenApiIgnore
        get() = this.id ?: "N/A"
}
