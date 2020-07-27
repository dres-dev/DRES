package dres.api.rest.types.run

import dres.api.rest.types.competition.RestTaskDescription
import dres.api.rest.types.competition.RestTeam
import dres.data.model.competition.Team
import dres.data.model.competition.TaskDescription
import dres.data.model.run.CompetitionRun
import dres.run.RunManager

/**
 * Contains the basic and most importantly static information about a [CompetitionRun] and the
 * associated [RunManager]. Since this information usually doesn't change in the course of a run,
 * it allows for local caching and other optimizations.
 *
 * @author Ralph Gasser
 * @version 1.0.1
 */
data class RunInfo(val id: String, val name: String, val description: String?, val teams: List<RestTeam>, val tasks: List<RestTaskDescription>) {
    constructor(run: RunManager) : this(run.id.string, run.name, run.competitionDescription.description, run.competitionDescription.teams.map { RestTeam(it) }, run.competitionDescription.tasks.map { RestTaskDescription.fromTask(it) })
}