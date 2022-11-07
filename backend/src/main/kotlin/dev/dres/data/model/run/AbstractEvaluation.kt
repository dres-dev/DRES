package dev.dres.data.model.run

import dev.dres.data.model.run.interfaces.EvaluationRun
import dev.dres.data.model.template.EvaluationTemplate
import dev.dres.data.model.run.interfaces.Run
import kotlinx.dnq.util.findById

/**
 * An abstract [Run] implementation that can be used by different subtypes.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractEvaluation(evaluation: Evaluation): EvaluationRun {

    /** The internal [xdId] of this [AbstractEvaluation].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory.
     */
    private val xdId = evaluation.xdId

    /**
     * Accessor for the [Evaluation] underpinning this [AbstractEvaluation]
     */
    protected val evaluation: Evaluation
        get() = Evaluation.findById(this.xdId)

    /** The [EvaluationId] of this [AbstractEvaluation].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory.
     */
    override val id: EvaluationId = evaluation.id

    /** The name of this [AbstractEvaluation].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory. *
     */
    override val name: String = evaluation.name

    /** The [EvaluationTemplate] used by this [AbstractEvaluation].
     *
     * Since this cannot change during the lifetime of an evaluation, it is stored in memory.
     */
    override val description: EvaluationTemplate = evaluation.template

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

    /**
     * Re-activates this [AbstractEvaluation]
     */
    override fun reactivate() {
        if (this.ended == null){
            throw IllegalStateException("Run has not yet ended.")
        }
        this.ended = null
    }
}