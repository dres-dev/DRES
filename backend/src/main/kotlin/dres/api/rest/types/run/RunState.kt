package dres.api.rest.types.run

import dres.api.rest.types.competition.RestTaskDescription
import dres.data.model.competition.TaskDescription
import dres.data.model.run.CompetitionRun
import dres.run.RunManager
import dres.run.RunManagerStatus

/**
 * Contains the information about the state of a [CompetitionRun] and the associated [RunManager].
 *
 * This is information that changes in the course of a run an therefore must be updated frequently.
 *
 * @author Ralph Gasser
 * @version 1.0.1
 */
data class RunState(val id: String, val status: RunManagerStatus, val currentTask: RestTaskDescription?, val timeLeft: Long) {
    constructor(run: RunManager) : this(run.id.string, run.status, run.currentTask?.let { RestTaskDescription.fromTask(it) }, run.timeLeft() / 1000)
}

