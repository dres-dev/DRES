package dres.run.score.scorer

import dres.data.model.run.CompetitionRun
import dres.data.model.run.SubmissionStatus
import dres.run.score.interfaces.RecalculatingTaskRunScorer
import kotlin.math.max

class KisTaskScorer : RecalculatingTaskRunScorer {

    private val maxPointsPerTask = 100.0
    private val maxPointsAtTaskEnd = 50.0
    private val penaltyPerWrongSubmission = 10.0

    private var lastScores: Map<Int, Double> = emptyMap()

    override fun analyze(task: CompetitionRun.TaskRun): Map<Int, Double> {

        val taskStart = task.started ?: return emptyMap()

        //actual duration of task, in case it was extended during competition
        val taskDuration = max(task.task.duration, (task.ended ?: 0) - taskStart ).toDouble()

        lastScores = task.submissions.groupBy { it.team }.map{

            //explicitly enforce valid types and order
            val sorted =  it.value.filter { it.status == SubmissionStatus.CORRECT || it.status == SubmissionStatus.WRONG }.sortedBy { it.timestamp }

            val firstCorrect = sorted.indexOfFirst { it.status == SubmissionStatus.CORRECT }

            val score = if (firstCorrect > -1) {
                val incorrectSubmissions = firstCorrect + 1
                val timeFraction = (sorted[firstCorrect].timestamp - taskStart) / taskDuration

                max(0.0,
                        maxPointsAtTaskEnd +
                                ((maxPointsPerTask - maxPointsAtTaskEnd) * timeFraction) -
                                (incorrectSubmissions * penaltyPerWrongSubmission)
                )
            } else {
                0.0
            }
            it.key to score
        }.toMap()

        return lastScores
    }

    override fun scores(): Map<Int, Double> = lastScores
}