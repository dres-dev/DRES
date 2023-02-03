package dev.dres.run.score.scorer

import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.run.score.TaskContext
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlin.math.max

class KisTaskScorer(
        private val maxPointsPerTask: Double = defaultmaxPointsPerTask,
        private val maxPointsAtTaskEnd: Double = defaultmaxPointsAtTaskEnd,
        private val penaltyPerWrongSubmission: Double = defaultpenaltyPerWrongSubmission
) : TaskScorer {

    constructor(parameters: Map<String, String>) : this(
        parameters.getOrDefault("maxPointsPerTask", "$defaultmaxPointsPerTask").toDoubleOrNull() ?: defaultmaxPointsPerTask,
        parameters.getOrDefault("maxPointsAtTaskEnd", "$defaultmaxPointsAtTaskEnd").toDoubleOrNull() ?: defaultmaxPointsAtTaskEnd,
        parameters.getOrDefault("penaltyPerWrongSubmission", "$defaultpenaltyPerWrongSubmission").toDoubleOrNull() ?: defaultpenaltyPerWrongSubmission
    )

    companion object {
        private const val defaultmaxPointsPerTask: Double = 1000.0
        private const val defaultmaxPointsAtTaskEnd: Double = 500.0
        private const val defaultpenaltyPerWrongSubmission: Double = 100.0
    }


    override fun computeScores(submissions: Sequence<DbSubmission>, context: TaskContext): Map<TeamId, Double>  {
        val taskStartTime = context.taskStartTime ?: throw IllegalArgumentException("No task start time specified.")
        val taskDuration = context.taskDuration ?: throw IllegalArgumentException("No task duration specified.")
        val tDur = max(taskDuration * 1000L, (context.taskEndTime ?: 0) - taskStartTime).toDouble() //actual duration of task, in case it was extended during competition
        return context.teamIds.associateWith { teamId ->
            val verdicts = submissions.filter { it.team.id == teamId }.sortedBy { it.timestamp }.flatMap { sub ->
                sub.verdicts.filter { (it.status eq DbVerdictStatus.CORRECT) or (it.status eq DbVerdictStatus.WRONG) }.asSequence()
            }.toList()
            val firstCorrect = verdicts.indexOfFirst { it.status == DbVerdictStatus.CORRECT }
            val score = if (firstCorrect > -1) {
                val timeFraction = 1.0 - (verdicts[firstCorrect].submission.timestamp - taskStartTime) / tDur
                max(
                    0.0,
                    this.maxPointsAtTaskEnd +
                            ((maxPointsPerTask - maxPointsAtTaskEnd) * timeFraction) -
                            (firstCorrect * penaltyPerWrongSubmission) //index of first correct submission is the same as number of not correct submissions
                )
            } else {
                0.0
            }
            score
        }
    }

}