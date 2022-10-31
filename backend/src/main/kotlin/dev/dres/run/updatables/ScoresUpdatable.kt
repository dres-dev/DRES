package dev.dres.run.updatables

import dev.dres.api.rest.types.run.websocket.ServerMessage
import dev.dres.api.rest.types.run.websocket.ServerMessageType
import dev.dres.data.model.UID
import dev.dres.data.model.run.AbstractInteractiveTask
import dev.dres.data.model.run.InteractiveSynchronousCompetition
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.run.RunManagerStatus
import dev.dres.run.score.TaskContext
import dev.dres.run.score.interfaces.IncrementalSubmissionTaskScorer
import dev.dres.run.score.interfaces.RecalculatingSubmissionTaskScorer
import java.util.*

/**
 * This is a [Updatable] that runs necessary post-processing after a [Submission] has been validated and
 * updates the scores for the respective [InteractiveSynchronousCompetition.Task].
 *
 * @author Ralph Gasser
 * @version 1.2.0
 */
class ScoresUpdatable(val runId: UID, val scoreboardsUpdatable: ScoreboardsUpdatable, val messageQueueUpdatable: MessageQueueUpdatable, val daoUpdatable: DAOUpdatable<*>): Updatable {

    companion object {
        val ELIGIBLE_STATUS = arrayOf(RunManagerStatus.ACTIVE/*, RunManagerStatus.RUNNING_TASK, RunManagerStatus.PREPARING_TASK, RunManagerStatus.TASK_ENDED*/)
    }

    /** Internal list of [Submission] that pend processing. */
    private val list = LinkedList<Pair<AbstractInteractiveTask, Submission>>()

    /** The [Phase] this [ScoresUpdatable] belongs to. */
    override val phase: Phase = Phase.MAIN

    /** Enqueues a new [Submission] for post-processing. */
    fun enqueue(submission: Pair<AbstractInteractiveTask,Submission>) = this.list.add(submission)

    override fun update(status: RunManagerStatus) {
        if (!this.list.isEmpty()) {
            val scorersToUpdate = mutableSetOf<Pair<AbstractInteractiveTask,RecalculatingSubmissionTaskScorer>>()
            val removed = this.list.removeIf {
                val scorer = it.first.scorer
                if (it.second.status != SubmissionStatus.INDETERMINATE) {
                    when(scorer) {
                        is RecalculatingSubmissionTaskScorer -> scorersToUpdate.add(Pair(it.first, scorer))
                        is IncrementalSubmissionTaskScorer -> scorer.update(it.second)
                        else -> { }
                    }
                    true
                } else {
                    false
                }
            }

            /* Update scorers. */
            scorersToUpdate.forEach {
                val task = it.first
                if (it.first.started != null) {
                    val scores = it.second.computeScores(task.submissions, TaskContext(task.competition.description.teams.map { t -> t.uid }, task.started, task.description.duration, task.ended))
                    it.first.updateTeamAggregation(scores)
                }
            }

            /* If elements were removed, then update scoreboards and tasks. */
            if (removed) {
                this.scoreboardsUpdatable.dirty = true
                this.daoUpdatable.dirty = true
                this.messageQueueUpdatable.enqueue(ServerMessage(this.runId.string, ServerMessageType.TASK_UPDATED))
            }
        }
    }

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = ELIGIBLE_STATUS.contains(status)
}