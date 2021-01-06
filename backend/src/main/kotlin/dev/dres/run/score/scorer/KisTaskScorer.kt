package dev.dres.run.score.scorer

import dev.dres.data.model.UID
import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.run.score.interfaces.RecalculatingTaskRunScorer
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.max

class KisTaskScorer(
        private val maxPointsPerTask: Double = 100.0,
        private val maxPointsAtTaskEnd: Double = 50.0,
        private val penaltyPerWrongSubmission: Double = 10.0
) : RecalculatingTaskRunScorer {

    private var lastScores: Map<UID, Double> = emptyMap()
    private val lastScoresLock = ReentrantReadWriteLock()

    override fun computeScores(submissions: Collection<Submission>, teamIds: Collection<UID>, taskStartTime: Long, taskDuration: Long, taskEndTime: Long): Map<UID, Double> = this.lastScoresLock.write {

        val tDur = max(taskDuration * 1000L, taskEndTime - taskStartTime).toDouble() //actual duration of task, in case it was extended during competition

        this.lastScores = teamIds.map { teamId ->
            val sbs =  submissions.filter { it.teamId == teamId && (it.status == SubmissionStatus.CORRECT || it.status == SubmissionStatus.WRONG) }.sortedBy { it.timestamp }
            val firstCorrect = sbs.indexOfFirst { it.status == SubmissionStatus.CORRECT }
            val score = if (firstCorrect > -1) {
                val timeFraction = 1.0 - (sbs[firstCorrect].timestamp - taskStartTime) / tDur

                max(0.0,
                        maxPointsAtTaskEnd +
                                ((maxPointsPerTask - maxPointsAtTaskEnd) * timeFraction) -
                                (firstCorrect * penaltyPerWrongSubmission) //index of first correct submission is the same as number of not correct submissions
                )
            } else {
                0.0
            }
            teamId to score
        }.toMap()

        return this.lastScores
    }

    override fun scores(): Map<UID, Double> = this.lastScoresLock.read { this.lastScores }
}