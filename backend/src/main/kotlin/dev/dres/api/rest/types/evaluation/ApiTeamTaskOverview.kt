package dev.dres.api.rest.types.evaluation

data class ApiTeamTaskOverview(val teamId: String, val tasks: List<ApiTaskOverview>)
