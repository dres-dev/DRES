package dev.dres.api.rest.types.evaluation

import kotlinx.serialization.Serializable

@Serializable
data class ApiTeamTaskOverview(val teamId: String, val tasks: List<ApiTaskOverview>)
