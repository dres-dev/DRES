package dres.api.rest.types.run

import dres.data.model.competition.Team
import dres.data.model.run.CompetitionRun
import dres.run.RunManager

/**
 * Contains the basic and most importantly static information about a [CompetitionRun] and the
 * associated [RunManager]. Since this information usually doesn't change in the course of a run,
 * it allows for local caching and other optimizations.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class RunInfo(val id: Long, val name: String, val description: String?, val teams: List<Team>) {
    constructor(run: RunManager) : this(run.runId, run.name, run.competition.description, run.competition.teams)
}

