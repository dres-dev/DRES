package dev.dres.run.updatables

import dev.dres.data.model.run.AbstractInteractiveTask
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbAnswerSet
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
 * @version 1.3.0
 */
class ScoresUpdatable(private val manager: RunManager): Updatable {

    companion object {
        private val ELIGIBLE_STATUS = arrayOf(RunManagerStatus.ACTIVE)
    }

    /** Internal list of [DbAnswerSet] that pend processing. */
    private val set = LinkedHashSet<AbstractInteractiveTask>()

    /** The [Phase] this [ScoresUpdatable] belongs to. */
    override val phase: Phase = Phase.MAIN

    /**
     * Enqueues a new [AbstractInteractiveTask] that requires score re-calculation.
     *
     * @param task The [AbstractInteractiveTask] tha requires score re-calculation.
     */
    @Synchronized
    fun enqueue(task: AbstractInteractiveTask) = this.set.add(task)

    @Synchronized
    override fun update(status: RunManagerStatus) {
        val removed = mutableListOf<TaskId>()
        if (this.set.isNotEmpty()) {
            this.manager.store.transactional(true) {
                this.set.removeIf { task ->
                    val submissions = DbAnswerSet.filter { a -> a.task.id eq task.taskId }.mapDistinct { it.submission }.asSequence()
                    val scores = task.scorer.scoreMap(submissions)
                    task.updateTeamAggregation(scores)
                    removed.add(task.taskId)
                    true
                }
            }
        }
    }

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = ELIGIBLE_STATUS.contains(status)
}