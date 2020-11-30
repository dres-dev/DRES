package dev.dres.run.score.interfaces

import dev.dres.data.model.UID
import dev.dres.data.model.run.CompetitionRun
import dev.dres.data.model.run.Submission

/**
 * Computes the current scores of all teams for a given  [CompetitionRun.TaskRun]
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.1
 */
interface RecalculatingTaskRunScorer: TaskRunScorer {

    fun computeScores(submissions: Collection<Submission>, teamIds: Collection<UID>, taskStartTime: Long, taskDuration: Long, taskEndTime: Long = 0): Map<UID, Double>

}