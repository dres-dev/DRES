package dev.dres.run.score.interfaces

import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.TaskContext

/**
 * A [TaskScorer] implementation that re-computes the current scores of all teams for a given [TaskRun] based on the
 * entire [Submission] history. As opposed to the [IncrementalSubmissionTaskScorer], incremental updates are not possible.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 1.1.0
 */
interface RecalculatingSubmissionTaskScorer: TaskScorer {
    /**
     * Re-computes this [RecalculatingSubmissionTaskScorer]'s score based on the given [Submission] history.
     *
     * @param submissions The [Submission]s used to update this [RecalculatingSubmissionTaskScorer] with.
     * @param context The [TaskContext] in which scoring takes place.
     */
    fun computeScores(submissions: Collection<Submission>, context: TaskContext): Map<TeamId, Double>
}