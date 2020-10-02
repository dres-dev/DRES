package dev.dres.run

import dev.dres.data.model.run.CompetitionRun


/**
 * Performs analysis on a [CompetitionRun.TaskRun]
 *
 * @author Luca Rossetto
 * @version 1.0
 */
interface TaskRunAnalyzer<out T> {
    /**
     * Analyses the given [CompetitionRun.TaskRun] and produces some result.
     *
     * @param task The [CompetitionRun.TaskRun] to analyse.
     */
    fun analyze(task: CompetitionRun.TaskRun): T
}