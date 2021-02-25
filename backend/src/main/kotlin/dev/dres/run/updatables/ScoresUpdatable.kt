package dev.dres.run.updatables

import dev.dres.api.rest.types.run.websocket.ServerMessage
import dev.dres.api.rest.types.run.websocket.ServerMessageType
import dev.dres.data.model.UID
import dev.dres.data.model.run.InteractiveSynchronousCompetitionRun
import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.run.RunManagerStatus
import dev.dres.run.score.interfaces.IncrementalTaskRunScorer
import dev.dres.run.score.interfaces.RecalculatingTaskRunScorer
import java.util.*

/**
 * This is a [Updatable] that runs necessary post-processing after a [Submission] has been validated;
 * it update the scores for the respective [InteractiveSynchronousCompetitionRun.TaskRun].
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class ScoresUpdatable(val runId: UID, val scoreboardsUpdatable: ScoreboardsUpdatable, val messageQueueUpdatable: MessageQueueUpdatable): Updatable {

    companion object {
        val ELIGIBLE_STATUS = arrayOf(RunManagerStatus.ACTIVE, RunManagerStatus.RUNNING_TASK, RunManagerStatus.PREPARING_TASK, RunManagerStatus.TASK_ENDED)
    }

    /** Internal list of [Submission] that pend processing. */
    private val list = LinkedList<Pair<InteractiveSynchronousCompetitionRun.TaskRun,Submission>>()

    /** The [Phase] this [ScoresUpdatable] belongs to. */
    override val phase: Phase = Phase.MAIN

    /** Enqueues a new [Submission] for post-processing. */
    fun enqueue(submission: Pair<InteractiveSynchronousCompetitionRun.TaskRun,Submission>) = this.list.add(submission)

    override fun update(status: RunManagerStatus) {
        if (!this.list.isEmpty()) {
            val scorersToUpdate = mutableSetOf<Pair<InteractiveSynchronousCompetitionRun.TaskRun,RecalculatingTaskRunScorer>>()
            val removed = this.list.removeIf {
                val scorer = it.first.scorer
                if (it.second.status != SubmissionStatus.INDETERMINATE) {
                    when(scorer) {
                        is RecalculatingTaskRunScorer -> scorersToUpdate.add(Pair(it.first, scorer))
                        is IncrementalTaskRunScorer -> scorer.update(it.second)
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
                    it.second.computeScores(task.submissions, task.competition.competitionDescription.teams.map { t -> t.uid }, task.started!!, task.duration, task.ended ?: 0)
                }
            }

            /* If elements were removed, then update scoreboards and tasks. */
            if (removed) {
                this.scoreboardsUpdatable.dirty = true
                this.messageQueueUpdatable.enqueue(ServerMessage(this.runId.string, ServerMessageType.TASK_UPDATED))
            }
        }
    }

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = ELIGIBLE_STATUS.contains(status)
}