package dres.api.rest.types.run

import dres.data.model.competition.interfaces.TaskDescription
import dres.data.model.run.CompetitionRun
import dres.run.RunManager
import dres.run.RunManagerStatus

/**
 * Contains the information about the state of a [CompetitionRun] and the associated [RunManager].
 *
 * This is information that changes in the course of a run an therefore must be updated frequently.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class RunState(val id: Long, val status: RunManagerStatus, val currentTask: TaskDescription?, val timeLeft: Long) {
    constructor(run: RunManager) : this(run.runId, run.status, run.currentTask, run.timeLeft())
}

