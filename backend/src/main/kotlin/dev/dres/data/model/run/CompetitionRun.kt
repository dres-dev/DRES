package dev.dres.data.model.run

import dev.dres.data.model.Entity
import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.run.InteractiveSynchronousCompetitionRun.TaskRun

typealias CompetitionRunId = UID


abstract class CompetitionRun(override var id: CompetitionRunId, val name: String, val competitionDescription: CompetitionDescription): Run, Entity {

    /** Timestamp of when this [InteractiveSynchronousCompetitionRun] was started. */
    @Volatile
    override var started: Long? = null
        protected set

    /** Timestamp of when this [TaskRun] was ended. */
    @Volatile
    override var ended: Long? = null
        protected set

    /**
     * Starts this [InteractiveSynchronousCompetitionRun].
     */
    open fun start() {
        if (this.hasStarted) {
            throw IllegalStateException("Competition run '$name' has already been started.")
        }
        this.started = System.currentTimeMillis()
    }

    /**
     * Ends this [InteractiveSynchronousCompetitionRun].
     */
    open fun end() {
        if (!this.isRunning) {
            this.started = System.currentTimeMillis()
        }
        this.ended = System.currentTimeMillis()
    }

    abstract val tasks: List<Task>

}