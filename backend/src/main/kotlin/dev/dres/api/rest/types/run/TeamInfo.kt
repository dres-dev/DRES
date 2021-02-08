package dev.dres.api.rest.types.run

import dev.dres.data.model.competition.Team
import dev.dres.data.model.run.InteractiveCompetitionRun

/**
 * Basic and most importantly static information about the [Team] partaking in a [InteractiveCompetitionRun].
 * Since this information usually doesn't change in the course of a run,t allows for local caching
 * and other optimizations.
 *
 * @author Ralph Gasser
 * @version 1.0.2
 */
data class TeamInfo(val uid: String, val name: String, val color: String, val logoId: String) {
    constructor(team: Team) : this(team.uid.string, team.name, team.color, team.logoId.string)
}