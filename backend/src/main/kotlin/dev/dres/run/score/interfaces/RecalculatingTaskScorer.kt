package dev.dres.run.score.interfaces

import dev.dres.data.model.UID
import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.run.interfaces.Task
import dev.dres.data.model.submissions.Submission

/**
 * A [TaskScorer] implementation that re-computes the current scores of all teams for a given [Task] based on the
 * entire [Submission] history. As opposed to the [IncrementalTaskScorer], incremental updates are not possible.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.2
 */
interface RecalculatingTaskScorer: TaskScorer {
    /**
     * Re-computes this [RecalculatingTaskScorer]'s score based on the given [Submission] history.
     *
     * @param submissions The [Submission]s used to update this [RecalculatingTaskScorer] with.
     * @param taskStartTime Time the [Task] started.
     * @param taskDuration Duration of the [Task].
     * @param taskEndTime Time the [Task] ended.
     *
     * TODO: Should we maybe introduce a "TaskContext" here instead of handing over these individual parameters?
     */
    fun computeScores(submissions: Collection<Submission>, teamIds: Collection<TeamId>, taskStartTime: Long, taskDuration: Long, taskEndTime: Long = 0): Map<UID, Double>
}