package dres.run.score.interfaces

import dres.data.model.run.CompetitionRun
import dres.run.TaskRunAnalyzer

/**
 * Computes the current scores of all teams for a given  [CompetitionRun.TaskRun]
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
interface RecalculatingTaskRunScorer: TaskRunAnalyzer<Map<Int, Double>>, TaskRunScorer