package dev.dres.data.model.run

import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.data.model.run.interfaces.EvaluationId
import dev.dres.data.model.run.interfaces.EvaluationRun
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.run.interfaces.Run
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.util.findById

/**
 * An abstract [Run] implementation that can be used by different subtypes.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractEvaluation(protected val store: TransientEntityStore, evaluation: DbEvaluation): EvaluationRun {

    /** The internal [xdId] of this [AbstractEvaluation].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory.
     */
    private val xdId = this.store.transactional (true) { evaluation.xdId }

    /** The [EvaluationId] of this [AbstractEvaluation].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory.
     */
    override val id: EvaluationId = this.store.transactional (true) { evaluation.id }

    /** The name of this [AbstractEvaluation].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory. *
     */
    override val name: String = this.store.transactional (true) {  evaluation.name }

    /** The [DbEvaluationTemplate] used by this [AbstractEvaluation].
     *
     * Since this cannot change during the lifetime of an evaluation, it is stored in memory.
     */
    override val template: ApiEvaluationTemplate = this.store.transactional (true) { evaluation.template.toApi() }

    /**
     * Accessor for the [DbEvaluation] underpinning this [AbstractEvaluation]
     */
    protected val evaluation: DbEvaluation
        get() = DbEvaluation.findById(this.xdId)

    /** Timestamp of when this [AbstractEvaluation] was started. */
    override var started: Long?
        get() = this.store.transactional (true) { this.evaluation.started }
        protected set(value) {
            this.evaluation.started = value
        }

    /** Timestamp of when this [AbstractEvaluation] was ended. */
    override var ended: Long?
        get() = this.store.transactional (true) { this.evaluation.ended }
        protected set(value) {
            this.evaluation.ended = value
        }

    /** Flag indicating that participants can also use the viewer for this [DbEvaluation]. */
    override var participantCanView: Boolean
        get() = this.store.transactional (true) { this.evaluation.participantCanView }
        internal set(value) { this.evaluation.participantCanView = value}


    /** Flag indicating that tasks can be repeated.*/
    override var allowRepeatedTasks: Boolean
        get() = this.store.transactional (true) { this.evaluation.allowRepeatedTasks }
        internal set(value) { this.evaluation.allowRepeatedTasks = value}


    /** A fixed limit on submission previews. */
    override var limitSubmissionPreviews: Int
        get() = this.store.transactional (true) { this.evaluation.limitSubmissionPreviews }
        internal set(value) {this.evaluation.limitSubmissionPreviews = value}


    /**
     * Starts this [AbstractEvaluation].
     */
    override fun start() {
        if (this.hasStarted) {
            throw IllegalStateException("Run has already been started.")
        }
        this.store.transactional {
            this.started = System.currentTimeMillis()
        }
    }

    /**
     * Ends this [AbstractEvaluation].
     */
    override fun end() {
        this.store.transactional {
            if (!this.isRunning) {
                this.started = System.currentTimeMillis()
            }
            this.ended = System.currentTimeMillis()
        }
    }

    /**
     * Re-activates this [AbstractEvaluation]
     */
    override fun reactivate() {
        this.store.transactional {
            if (this.ended == null) {
                throw IllegalStateException("Run has not yet ended.")
            }
            this.ended = null
        }
    }
}