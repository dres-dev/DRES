package dres.run.updatables

import dres.api.rest.types.run.websocket.ServerMessage
import dres.api.rest.types.run.websocket.ServerMessageType
import dres.data.model.run.CompetitionRun
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import dres.run.RunManagerStatus
import dres.run.score.interfaces.IncrementalTaskRunScorer
import dres.run.score.interfaces.RecalculatingTaskRunScorer
import java.util.*

/**
 * This is a [Updatable] that runs necessary post-processing after a [Submission] has been validated;
 * it update the scores for the respective [CompetitionRun.TaskRun].
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class ScoresUpdatable(val runId: Long, val scoreboardsUpdatable: ScoreboardsUpdatable, val messageQueueUpdatable: MessageQueueUpdatable): Updatable {

    companion object {
        val ELIGIBLE_STATUS = arrayOf(RunManagerStatus.ACTIVE, RunManagerStatus.RUNNING_TASK, RunManagerStatus.PREPARING_TASK, RunManagerStatus.TASK_ENDED)
    }

    /** Internal list of [Submission] that pend processing. */
    private val list = LinkedList<Pair<CompetitionRun.TaskRun,Submission>>()

    /** The [Phase] this [ScoresUpdatable] belongs to. */
    override val phase: Phase = Phase.MAIN

    /** Enqueues a new [Submission] for post-processing. */
    fun enqueue(submission: Pair<CompetitionRun.TaskRun,Submission>) = this.list.add(submission)

    override fun update(status: RunManagerStatus) {
        val scorersToUpdate = mutableSetOf<RecalculatingTaskRunScorer>()
        if (!this.list.isEmpty()) {
            this.list.removeIf {
                val scorer = it.first.scorer
                if (it.second.status != SubmissionStatus.INDETERMINATE) {
                    when(scorer) {
                        is RecalculatingTaskRunScorer -> scorersToUpdate.add(scorer)
                        is IncrementalTaskRunScorer -> scorer.update(it.second)
                        else -> { }
                    }
                    true
                } else {
                    false
                }
            }

            /* Since Scoreboards depend on task scores, mark respective updateable as dirty. */
            this.scoreboardsUpdatable.dirty = true

            /* Enqueue WS message for sending */
            this.messageQueueUpdatable.enqueue(ServerMessage(this.runId, ServerMessageType.TASK_UPDATED))
        }
    }

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = ELIGIBLE_STATUS.contains(status)
}