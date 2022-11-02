package dev.dres.data.model.run


import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.run.interfaces.Run

/**
 * An abstract [Run] implementation that can be used by different subtypes.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractEvaluation(protected val evaluation: Evaluation): dev.dres.data.model.run.interfaces.EvaluationRun {

    /** The name of this [AbstractEvaluation]. */
    override val id: EvaluationId
        get() = this.evaluation.id

    /** The name of this [AbstractEvaluation]. */
    override val name: String
        get() = this.evaluation.name

    /** The [CompetitionDescription] of this [AbstractEvaluation]. */
    override val description: CompetitionDescription
        get() = this.evaluation.description

    /** Timestamp of when this [AbstractEvaluation] was started. */
    override var started: Long
        get() = this.evaluation.started
        protected set(value) {
            this.evaluation.started = value
        }

    /** Timestamp of when this [AbstractEvaluation] was ended. */
    override var ended: Long?
        get() = this.evaluation.ended
        protected set(value) {
            this.evaluation.ended = value
        }

    /** Flag indicating that participants can also use the viewer for this [Evaluation]. */
    override var participantCanView: Boolean
        get() = this.evaluation.participantCanView
        set(value) {
            this.evaluation.participantCanView = value
        }

    /** Flag indicating that tasks can be repeated.*/
    override var allowRepeatedTasks: Boolean
        get() = this.evaluation.allowRepeatedTasks
        set(value) {
            this.evaluation.allowRepeatedTasks = value
        }

    /** A fixed limit on submission previews. */
    override var limitSubmissionPreviews: Int
        get() = this.evaluation.limitSubmissionPreviews
        set(value) {
            this.evaluation.limitSubmissionPreviews = value
        }

    /**
     * Starts this [AbstractEvaluation].
     */
    override fun start() {
        if (this.hasStarted) {
            throw IllegalStateException("Run has already been started.")
        }
        this.started = System.currentTimeMillis()
    }

    /**
     * Ends this [AbstractEvaluation].
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