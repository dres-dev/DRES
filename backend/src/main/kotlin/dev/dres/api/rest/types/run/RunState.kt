package dev.dres.api.rest.types.run

import dev.dres.data.model.run.InteractiveSynchronousCompetitionRun
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.InteractiveRunManager
import dev.dres.run.RunManager
import dev.dres.run.RunManagerStatus

/**
 * Contains the information about the state of a [InteractiveSynchronousCompetitionRun] and the associated [RunManager].
 *
 * This is information that changes in the course of a run an therefore must be updated frequently.
 *
 * @author Ralph Gasser
 * @version 1.0.2
 */
data class RunState(val id: String, val status: RunManagerStatus, val currentTask: TaskInfo?, val timeLeft: Long) {
    constructor(run: InteractiveRunManager, context: RunActionContext) : this(run.id.string, run.status, run.currentTask(context)?.let { TaskInfo(it) }, run.timeLeft(context) / 1000)
}

