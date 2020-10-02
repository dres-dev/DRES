package dev.dres.api.rest.types.run

import dev.dres.data.model.run.CompetitionRun
import dev.dres.run.RunManager

/**
 * Contains the basic and most importantly static information about a [CompetitionRun] and the
 * associated [RunManager]. Since this information usually doesn't change in the course of a run,
 * it allows for local caching and other optimizations.
 *
 * @author Ralph Gasser
 * @version 1.0.2
 */
data class RunInfo(
        val id: String,
        val name: String,
        val description: String?,
        val teams: List<TeamInfo>,
        val tasks: List<TaskInfo>,
        val competitionId: String,
        val participantsCanView: Boolean) {
    constructor(run: RunManager) : this(
            run.id.string,
            run.name,
            run.competitionDescription.description,
            run.competitionDescription.teams.map { TeamInfo(it) },
            run.competitionDescription.tasks.map { TaskInfo(it) },
            run.competitionDescription.id.string,
            run.competitionDescription.participantCanView
    )
}