package dev.dres.data.model.run

import dev.dres.api.rest.types.evaluation.ApiTaskStatus
import dev.dres.api.rest.types.evaluation.submission.ApiSubmission
import dev.dres.api.rest.types.template.tasks.ApiTaskTemplate
import dev.dres.data.model.run.interfaces.Run
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.template.TemplateId
import dev.dres.data.model.template.task.TaskTemplateId
import dev.dres.data.model.template.team.TeamAggregatorImpl
import dev.dres.data.model.template.team.TeamGroupId
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.filter.basics.SubmissionFilter
import dev.dres.run.transformer.basics.SubmissionTransformer
import dev.dres.run.validation.interfaces.AnswerSetValidator
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.mapDistinct
import kotlinx.dnq.util.findById

/**
 * An abstract [Run] implementation that can be used by different subtypes.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
abstract class AbstractTask(protected val store: TransientEntityStore, task: DbTask) : TaskRun {

    /** The internal [xdId] of this [AbstractEvaluation].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory.
     */
    private val xdId: String = this.store.transactional(true) { task.xdId }

    /**
     * Accessor for the [DbTask] underpinning this [AbstractTask]
     */
    protected val dbTask: DbTask
        get() = DbTask.findById(this.xdId)

    /**
     * The [TaskId] of this [AbstractTask].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory.
     */
    final override val taskId: TaskId = this.store.transactional(true) { task.taskId }

    /**
     * The [TaskTemplateId] of this [AbstractTask].
     *
     * Since this cannot change during the lifetime of an evaluation, it is kept in memory.
     */
    final override val taskTemplateId: TaskTemplateId = this.store.transactional(true) { task.template.templateId }

    /**
     * Reference to the [ApiTaskTemplate] describing this [AbstractTask].
     *
     * Requires active database transaction!
     */
    final override val template: ApiTaskTemplate = this.store.transactional(true) { this.dbTask.template.toApi() }

    /** The current [DbTaskStatus] of this [AbstractTask]. This is a transient property. */
    final override var status: ApiTaskStatus
        get() = this.store.transactional(true) { this.dbTask.status.toApi() }
        protected set(value) {
            this.dbTask.status = value.toDb()  /* Update backing database field. */
        }

    /**
     * Timestamp of when this [AbstractTask] was started.
     *
     * Setter requires active database transaction!
     */
    final override var started: Long?
        get() = this.store.transactional(true) { this.dbTask.started }
        protected set(value) {
            this.dbTask.started = value  /* Update backing database field. */
        }

    /**
     * Timestamp of when this [AbstractTask] was ended.
     *
     * Setter requires active database transaction!
     */
    final override var ended: Long?
        get() = this.store.transactional(true) { this.dbTask.ended }
        protected set(value) {
            this.dbTask.ended = value /* Update backing database field. */
        }


    /** The [SubmissionFilter] used to filter [DbSubmission]s. */
    abstract val filter: SubmissionFilter

    /** The [SubmissionTransformer] used to convert [DbSubmission]s. */
    abstract val transformer: SubmissionTransformer

    /** The [AnswerSetValidator] used to validate [DbAnswerSet]s. */
    abstract val validator: AnswerSetValidator

    /**
     * Prepares this [TaskRun] for later starting.
     */
    override fun prepare() {
        if (this.hasEnded) {
            throw IllegalStateException("Run has already ended.")
        }
        if (this.hasStarted) {
            throw IllegalStateException("Run has already been started.")
        }
        this.store.transactional {
            this.status = ApiTaskStatus.PREPARING
        }
    }

    /**
     * Starts this [AbstractTask].
     */
    override fun start() {
        if (this.started != null || this.status == ApiTaskStatus.RUNNING) {
            throw IllegalStateException("Run has already been started.")
        }
        this.store.transactional {
            this.started = System.currentTimeMillis()
            this.status = ApiTaskStatus.RUNNING
        }
    }

    /**
     * Ends this [AbstractTask].
     */
    override fun end() {
        this.store.transactional {
            if (this.started == null) {
                this.started = System.currentTimeMillis()
            }
            this.ended = System.currentTimeMillis()
            this.status = ApiTaskStatus.ENDED
        }
    }

    /**
     * Reactivates this [AbstractTask].
     */
    override fun reactivate() {
        this.store.transactional {
            if (this.ended == null) {
                throw IllegalStateException("Run has not yet ended.")
            }
            this.ended = null
            this.status = ApiTaskStatus.RUNNING
        }
    }

    /** Returns a [Sequence] of all [DbSubmission]s connected to this [AbstractTask]. */
    fun getDbSubmissions() = DbAnswerSet.filter { a ->
        a.task.id eq this@AbstractTask.taskId
    }.mapDistinct {
        it.submission
    }.asSequence()

    /** Map of [TeamGroupId] to [TeamAggregatorImpl]. */
    val teamGroupAggregators: Map<TeamGroupId, TeamAggregatorImpl> by lazy {
        this.evaluationRun.template.teamGroups.associate { it.id!! to it.newAggregator() }
    }

    /**
     * Updates the per-team aggregation for this [AbstractInteractiveTask].
     *
     * @param teamScores Map of team scores.
     */
    internal fun updateTeamAggregation(teamScores: Map<TeamId, Double>) {
        this.teamGroupAggregators.values.forEach { it.aggregate(teamScores) }
    }

    override fun getSubmissions(): List<ApiSubmission> = this.store.transactional(true) {
        this.getDbSubmissions().map { it.toApi() }.toList()
    }
}