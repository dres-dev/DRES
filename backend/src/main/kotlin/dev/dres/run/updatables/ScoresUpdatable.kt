package dev.dres.run.updatables

import dev.dres.api.rest.types.evaluation.ApiEvaluationState
import dev.dres.api.rest.types.evaluation.ApiTaskStatus
import dev.dres.api.rest.types.evaluation.websocket.ServerMessage
import dev.dres.api.rest.types.evaluation.websocket.ServerMessageType
import dev.dres.data.model.run.AbstractInteractiveTask
import dev.dres.data.model.run.AbstractTask
import dev.dres.data.model.run.RunActionContext
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.run.RunExecutor
import dev.dres.run.RunManager
import dev.dres.run.RunManagerStatus
import dev.dres.run.score.Scoreable
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.mapDistinct
import kotlin.collections.LinkedHashSet

/**
 * This is a [Updatable] that runs necessary post-processing after a [DbSubmission] has been validated and updates the scores for the respective [Scoreable].
 *
 * @author Ralph Gasser
 * @version 1.3.1
 */
class ScoresUpdatable(private val manager: RunManager): Updatable {

    /** Internal list of [DbAnswerSet] that pend processing. */
    private val set = LinkedHashSet<AbstractTask>()

    /** The [Phase] this [ScoresUpdatable] belongs to. */
    override val phase: Phase = Phase.MAIN

    /**
     * Enqueues a new [AbstractInteractiveTask] that requires score re-calculation.
     *
     * @param task The [AbstractInteractiveTask] tha requires score re-calculation.
     */
    @Synchronized
    fun enqueue(task: AbstractTask) = this.set.add(task)

    @Synchronized
    override fun update(runStatus: RunManagerStatus, taskStatus: ApiTaskStatus?, context: RunActionContext) {
        val removed = mutableListOf<TaskId>()
        if (this.set.isNotEmpty()) {
            this.manager.store.transactional(true) {
                /* Update scores. */
                this.set.removeIf { task ->
                    val submissions = DbAnswerSet.filter { a -> a.task.id eq task.taskId }.mapDistinct { it.submission }.asSequence()
                    val scores = task.scorer.scoreMap(submissions)
                    task.updateTeamAggregation(scores)
                    removed.add(task.taskId)
                    true
                }
            }

            /* Inform clients about changes. */
            removed.forEach { RunExecutor.broadcastWsMessage(ServerMessage(this.manager.evaluation.id, ServerMessageType.TASK_UPDATED, it)) }
        }
    }

    /**
     * Returns true, if the [ScoresUpdatable] should be updated given the [RunManagerStatus]
     * and the [ApiEvaluationState]. The [ScoresUpdatable] is always triggered if the run is ongoing.
     *
     * @param runStatus The [RunManagerStatus] to check.
     * @param taskStatus The [ApiTaskStatus] to check. Can be null
     * @return True if update is required, which is while a task is running.
     */
    override fun shouldBeUpdated(runStatus: RunManagerStatus, taskStatus: ApiTaskStatus?): Boolean
        = (runStatus == RunManagerStatus.ACTIVE)
}