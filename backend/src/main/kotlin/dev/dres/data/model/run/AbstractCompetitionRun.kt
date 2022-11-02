package dev.dres.data.model.run


import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.run.interfaces.Run

/**
 * An abstract [Run] implementation that can be used by different subtypes.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractCompetitionRun(protected val competition: Competition): dev.dres.data.model.run.interfaces.Competition {

    /** The name of this [AbstractCompetitionRun]. */
    override val id: CompetitionId
        get() = this.competition.id

    /** The name of this [AbstractCompetitionRun]. */
    override val name: String
        get() = this.competition.name

    /** The [CompetitionDescription] of this [AbstractCompetitionRun]. */
    override val description: CompetitionDescription
        get() = this.competition.description

    /** Timestamp of when this [AbstractCompetitionRun] was started. */
    override var started: Long
        get() = this.competition.started
        protected set(value) {
            this.competition.started = value
        }

    /** Timestamp of when this [AbstractCompetitionRun] was ended. */
    override var ended: Long?
        get() = this.competition.ended
        protected set(value) {
            this.competition.ended = value
        }

    /** Flag indicating that participants can also use the viewer for this [Competition]. */
    override var participantCanView: Boolean
        get() = this.competition.participantCanView
        set(value) {
            this.competition.participantCanView = value
        }

    /** Flag indicating that tasks can be repeated.*/
    override var allowRepeatedTasks: Boolean
        get() = this.competition.allowRepeatedTasks
        set(value) {
            this.competition.allowRepeatedTasks = value
        }

    /** A fixed limit on submission previews. */
    override var limitSubmissionPreviews: Int
        get() = this.competition.limitSubmissionPreviews
        set(value) {
            this.competition.limitSubmissionPreviews = value
        }

    /**
     * Starts this [AbstractCompetitionRun].
     */
    override fun start() {
        if (this.hasStarted) {
            throw IllegalStateException("Run has already been started.")
        }
        this.started = System.currentTimeMillis()
    }

    /**
     * Ends this [AbstractCompetitionRun].
     */
    override fun end() {
        if (!this.isRunning) {
            this.started = System.currentTimeMillis()
        }
        this.ended = System.currentTimeMillis()
    }

    override fun reactivate() {
        if (this.ended == null){
            throw IllegalStateException("Run has not yet ended.")
        }
        this.ended = null
    }
}