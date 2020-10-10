package dev.dres.api.rest.types.run

import dev.dres.data.model.competition.Team
import dev.dres.data.model.run.CompetitionRun

/**
 * Basic and most importantly static information about the [Team] partaking in a [CompetitionRun].
 * Since this information usually doesn't change in the course of a run,t allows for local caching
 * and other optimizations.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class TeamInfo(val name: String, val color: String) {
    constructor(team: Team) : this(team.name, team.color)
}