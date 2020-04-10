package dres.run.score

import dres.data.model.competition.Team
import dres.data.model.run.CompetitionRun
import dres.data.model.run.SubmissionStatus
import kotlin.math.max

class KisTaskScorer(private val run: CompetitionRun): TaskRunScorer {

    private val maxPointsPerTask = 100.0
    private val maxPointsAtTaskEnd = 50.0
    private val penaltyPerWrongSubmission = 20.0

    override fun analyze(task: CompetitionRun.TaskRun): Map<Team, Double> {

        val taskStart = task.started ?: return emptyMap()

        //actual duration of task, in case it was extended during competition
        val taskDuration = max(task.task.duration, (task.ended ?: 0) - taskStart ).toDouble()

        return task.submissions.groupBy { it.team }.map{

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
            run.competition.teams[it.key] to score

        }.toMap()
    }
}