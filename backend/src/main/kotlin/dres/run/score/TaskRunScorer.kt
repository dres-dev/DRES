package dres.run.score

import dres.data.model.competition.Team
import dres.data.model.run.CompetitionRun
import dres.run.TaskRunAnalyzer

/**
 * Computes the current scores of all teams for a given  [CompetitionRun.TaskRun]
 */
interface TaskRunScorer: TaskRunAnalyzer<Map<Team, Double>> {

    //TODO some possibility for incremental updates

}