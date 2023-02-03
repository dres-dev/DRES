package dev.dres.run.updatables

import dev.dres.api.rest.types.evaluation.websocket.ServerMessage
import dev.dres.api.rest.types.evaluation.websocket.ServerMessageType
import dev.dres.data.model.run.AbstractInteractiveTask
import dev.dres.data.model.run.EvaluationId
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.run.RunManagerStatus
import dev.dres.run.score.TaskContext
import dev.dres.run.score.interfaces.IncrementalSubmissionTaskScorer
import dev.dres.run.score.interfaces.RecalculatingSubmissionTaskScorer
import kotlinx.dnq.query.asSequence
import java.util.*

/**
 * This is a [Updatable] that runs necessary post-processing after a [DbSubmission] has been validated and updates the scores for the respective [TaskContext].
 *
 * @author Ralph Gasser
 * @version 1.2.0
 */
class ScoresUpdatable(private val evaluationId: EvaluationId, private val scoreboardsUpdatable: ScoreboardsUpdatable, private val messageQueueUpdatable: MessageQueueUpdatable): Updatable {

    companion object {
        private val ELIGIBLE_STATUS = arrayOf(RunManagerStatus.ACTIVE)
    }

    /** Internal list of [DbAnswerSet] that pend processing. */
    private val list = LinkedList<Pair<AbstractInteractiveTask, DbSubmission>>()

    /** The [Phase] this [ScoresUpdatable] belongs to. */
    override val phase: Phase = Phase.MAIN

    /** Enqueues a new [DbAnswerSet] for post-processing. */
    fun enqueue(submission: Pair<AbstractInteractiveTask,DbSubmission>) = this.list.add(submission)

    override fun update(status: RunManagerStatus) {
        if (!this.list.isEmpty()) {
            val scorersToUpdate = mutableSetOf<Pair<AbstractInteractiveTask,RecalculatingSubmissionTaskScorer>>()
            val removed = this.list.removeIf {
                when(val scorer = it.first.scorer) {
                    is RecalculatingSubmissionTaskScorer -> scorersToUpdate.add(Pair(it.first, scorer))
                    is IncrementalSubmissionTaskScorer -> scorer.update(it.second)
                    else -> { }
                }
                true
            }

            /* Update scorers. */
            scorersToUpdate.forEach {
                val task = it.first
                val scores = it.second.computeScores(
                    task.getSubmissions(),
                    TaskContext(task.id, task.competition.description.teams.asSequence().map { t -> t.id }.toList(), task.started, task.template.duration, task.ended)
                )
                it.first.updateTeamAggregation(scores)
            }

            /* If elements were removed, then update scoreboards and tasks. */
            if (removed) {
                this.scoreboardsUpdatable.dirty = true
                this.messageQueueUpdatable.enqueue(ServerMessage(this.evaluationId, ServerMessageType.TASK_UPDATED))
            }
        }
    }

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = ELIGIBLE_STATUS.contains(status)
}