package dev.dres.api.rest.types.evaluation

import dev.dres.run.*
import kotlinx.dnq.query.asSequence

data class ApiEvaluationOverview(val state: RunManagerStatus, val teamOverviews: List<ApiTeamTaskOverview>) {
    companion object {
        fun of(manager: InteractiveRunManager): ApiEvaluationOverview {
            val teamOverviews = when (manager) {
                is InteractiveSynchronousRunManager -> {
                    val overview = manager.evaluation.tasks.asSequence().map { ApiTaskOverview(it) }.toList()
                    manager.template.teams.asSequence().map {
                        ApiTeamTaskOverview(it.teamId, overview)
                    }.toList()
                }
                is InteractiveAsynchronousRunManager -> {
                    manager.evaluation.tasks.groupBy { it.teamId }.map { (teamId, tasks) ->
                        val overview = tasks.map { ApiTaskOverview(it) }
                        ApiTeamTaskOverview(teamId, overview)
                    }
                }
                else -> throw IllegalStateException("Unsupported run manager type") //should never happen
            }

            return ApiEvaluationOverview(manager.status, teamOverviews)
        }
    }
}



