package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.template.team.Team
import dev.dres.data.model.run.InteractiveSynchronousEvaluation
import dev.dres.data.model.template.team.TeamId

/**
 * Basic and most importantly static information about the [Team] partaking in a [InteractiveSynchronousEvaluation].
 * Since this information usually doesn't change in the course of a run,t allows for local caching
 * and other optimizations.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
data class ApiTeamInfo(val id: TeamId, val name: String, val color: String) {
    constructor(team: Team) : this(team.id, team.name, team.color)
}