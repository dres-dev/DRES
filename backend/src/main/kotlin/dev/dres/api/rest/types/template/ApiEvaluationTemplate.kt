package dev.dres.api.rest.types.template

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.api.rest.types.template.tasks.ApiTaskGroup
import dev.dres.api.rest.types.template.tasks.ApiTaskType
import dev.dres.api.rest.types.template.team.ApiTeam
import dev.dres.api.rest.types.template.team.ApiTeamGroup
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.TemplateId
import io.javalin.openapi.OpenApiIgnore
import kotlinx.serialization.Serializable

/**
 * The RESTful API equivalent for [DbEvaluationTemplate].
 *
 * @see DbEvaluationTemplate
 * @author Luca Rossetto & Ralph Gasser
 * @version 2.0.0
 */
@Serializable
data class ApiEvaluationTemplate(
    val id: TemplateId,
    val name: String,
    val description: String?,
    val created: Long?,
    val modified: Long?,
    val taskTypes: List<ApiTaskType>,
    val taskGroups: List<ApiTaskGroup>,
    val tasks: List<ApiTaskTemplate>,
    val teams: List<ApiTeam>,
    val teamGroups: List<ApiTeamGroup>,
    val judges: List<String>,
) {

    @get:JsonIgnore
    @delegate:JsonIgnore
    @get:OpenApiIgnore
    //@kotlinx.serialization.Transient not needed due to delegation
    private val allParticipantIds by lazy { teams.flatMap { team -> team.users.mapNotNull { it.id } }.toSet() }

    @JsonIgnore
    @OpenApiIgnore
    fun hasParticipant(userId: UserId): Boolean = allParticipantIds.contains(userId)

}

