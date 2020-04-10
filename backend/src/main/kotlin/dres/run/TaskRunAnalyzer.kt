package dres.run

import dres.data.model.run.CompetitionRun


/**
 * Performs analysis on a [CompetitionRun.TaskRun]
 */
interface TaskRunAnalyzer<out T> {

    fun analyze(task: CompetitionRun.TaskRun): T

}