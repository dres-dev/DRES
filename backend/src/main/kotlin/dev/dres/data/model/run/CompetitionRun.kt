package dev.dres.data.model.run

import dev.dres.data.model.Entity
import dev.dres.data.model.UID
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.run.InteractiveCompetitionRun.TaskRun

typealias CompetitionRunId = UID
typealias TaskRunId = UID

abstract class CompetitionRun(override var id: CompetitionRunId, val name: String, val competitionDescription: CompetitionDescription): Run, Entity {

    /** Timestamp of when this [InteractiveCompetitionRun] was started. */
    @Volatile
    override var started: Long? = null
        protected set

    /** Timestamp of when this [TaskRun] was ended. */
    @Volatile
    override var ended: Long? = null
        protected set

    /**
     * Starts this [InteractiveCompetitionRun].
     */
    open fun start() {
        if (this.hasStarted) {
            throw IllegalStateException("Competition run '$name' has already been started.")
        }
        this.started = System.currentTimeMillis()
    }

    /**
     * Ends this [InteractiveCompetitionRun].
     */
    open fun end() {
        if (!this.isRunning) {
            this.started = System.currentTimeMillis()
        }
        this.ended = System.currentTimeMillis()
    }

    abstract val tasks: List<Task>

}