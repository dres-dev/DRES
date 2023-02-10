package dev.dres.run.score.scorer

import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.TaskContext

/** Type alias for a */
typealias ScoreEntry = Triple<TeamId, String?, Double>

/**
 * A [TaskScorer] that re-computes the current scores of all teams for a given [TaskRun] based on the
 * entire [Submission] history.
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 1.1.0
 */
interface TaskScorer {
    /**
     * Re-computes this [TaskScorer]'s score based on the given [Submission] history.
     *
     * @param submissions The [Submission]s used to update this [TaskScorer] with.
     * @param context The [TaskContext] in which scoring takes place.
     */
    fun computeScores(submissions: Sequence<Submission>, context: TaskContext): Map<TeamId, Double>
}