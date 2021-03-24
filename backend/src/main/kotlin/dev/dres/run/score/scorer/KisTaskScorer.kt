package dev.dres.run.score.scorer

import dev.dres.data.model.UID
import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.run.score.ScoreEntry
import dev.dres.run.score.TaskContext
import dev.dres.run.score.interfaces.RecalculatingSubmissionTaskScorer
import dev.dres.run.score.interfaces.TeamTaskScorer
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.max

class KisTaskScorer(
        private val maxPointsPerTask: Double = defaultmaxPointsPerTask,
        private val maxPointsAtTaskEnd: Double = defaultmaxPointsAtTaskEnd,
        private val penaltyPerWrongSubmission: Double = defaultpenaltyPerWrongSubmission
) : RecalculatingSubmissionTaskScorer, TeamTaskScorer {

    constructor(parameters: Map<String, String>) : this(
        parameters.getOrDefault("maxPointsPerTask", "$defaultmaxPointsPerTask").toDoubleOrNull() ?: defaultmaxPointsPerTask,
        parameters.getOrDefault("maxPointsAtTaskEnd", "$defaultmaxPointsAtTaskEnd").toDoubleOrNull() ?: defaultmaxPointsAtTaskEnd,
        parameters.getOrDefault("penaltyPerWrongSubmission", "$defaultpenaltyPerWrongSubmission").toDoubleOrNull() ?: defaultpenaltyPerWrongSubmission
    )

    companion object {
        private const val defaultmaxPointsPerTask: Double = 100.0
        private const val defaultmaxPointsAtTaskEnd: Double = 50.0
        private const val defaultpenaltyPerWrongSubmission: Double = 10.0
    }

    private var lastScores: Map<TeamId, Double> = emptyMap()
    private val lastScoresLock = ReentrantReadWriteLock()

    override fun computeScores(submissions: Collection<Submission>, context: TaskContext): Map<UID, Double> = this.lastScoresLock.write {

        val taskStartTime = context.taskStartTime ?: throw IllegalArgumentException("no task start time specified")
        val taskDuration = context.taskDuration ?: throw IllegalArgumentException("no task duration specified")

        val tDur = max(taskDuration * 1000L, (context.taskEndTime ?: 0) - taskStartTime).toDouble() //actual duration of task, in case it was extended during competition

        this.lastScores = context.teamIds.map { teamId ->
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

    override fun teamScoreMap(): Map<TeamId, Double> = this.lastScoresLock.read { this.lastScores }

    override fun scores(): List<ScoreEntry> = this.lastScoresLock.read {
        this.lastScores.map { ScoreEntry(it.key, null, it.value) }
    }
}