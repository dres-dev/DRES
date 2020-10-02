package dev.dres.run.score.interfaces

import dev.dres.data.model.run.CompetitionRun
import dev.dres.run.TaskRunAnalyzer

/**
 * Computes the current scores of all teams for a given  [CompetitionRun.TaskRun]
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0
 */
interface RecalculatingTaskRunScorer: TaskRunAnalyzer<Map<Int, Double>>, TaskRunScorer