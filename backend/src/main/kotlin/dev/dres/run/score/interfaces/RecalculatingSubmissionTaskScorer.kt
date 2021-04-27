package dev.dres.run.score.interfaces

import dev.dres.data.model.UID
import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.run.interfaces.Task
import dev.dres.data.model.submissions.Submission
import dev.dres.run.score.TaskContext

/**
 * A [TaskScorer] implementation that re-computes the current scores of all teams for a given [Task] based on the
 * entire [Submission] history. As opposed to the [IncrementalSubmissionTaskScorer], incremental updates are not possible.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.2
 */
interface RecalculatingSubmissionTaskScorer: TaskScorer {
    /**
     * Re-computes this [RecalculatingSubmissionTaskScorer]'s score based on the given [Submission] history.
     *
     * @param submissions The [Submission]s used to update this [RecalculatingSubmissionTaskScorer] with.
     * @param taskStartTime Time the [Task] started.
     * @param taskDuration Duration of the [Task].
     * @param taskEndTime Time the [Task] ended.
     *
     */
    fun computeScores(submissions: Collection<Submission>, context: TaskContext): Map<TeamId, Double>
}