package dev.dres.api.rest.types.run

import dev.dres.data.model.run.InteractiveSynchronousCompetition
import dev.dres.data.model.run.RunProperties
import dev.dres.run.InteractiveAsynchronousRunManager
import dev.dres.run.RunManager

/**
 * Contains the basic and most importantly static information about a [InteractiveSynchronousCompetition] and the
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
        val properties: RunProperties,
        val type: RunType
        ) {
    constructor(run: RunManager) : this(
            run.id.string,
            run.name,
            run.description.description,
            run.description.teams.map { TeamInfo(it) },
            run.description.tasks.map { TaskInfo(it) },
            run.description.id.string,
            run.runProperties,
            if (run is InteractiveAsynchronousRunManager) {
                    RunType.ASYNCHRONOUS
            } else {
                    RunType.SYNCHRONOUS
            }
    )
}