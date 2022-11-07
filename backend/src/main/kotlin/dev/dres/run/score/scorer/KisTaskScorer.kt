package dev.dres.run.score.scorer

import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.run.score.TaskContext
import dev.dres.run.score.interfaces.RecalculatingSubmissionTaskScorer
import dev.dres.run.score.interfaces.ScoreEntry
import dev.dres.run.score.interfaces.TeamTaskScorer
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.toList
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

    override fun computeScores(submissions: Collection<Submission>, context: TaskContext): Map<TeamId, Double> = this.lastScoresLock.write {
        val taskStartTime = context.taskStartTime ?: throw IllegalArgumentException("No task start time specified.")
        val taskDuration = context.taskDuration ?: throw IllegalArgumentException("No task duration specified.")
        val tDur = max(taskDuration * 1000L, (context.taskEndTime ?: 0) - taskStartTime).toDouble() //actual duration of task, in case it was extended during competition
        this.lastScores = context.teamIds.associateWith { teamId ->
            val verdicts = submissions.filter { it.team.id == teamId }.sortedBy { it.timestamp }.flatMap { sub ->
                sub.verdicts.filter { (it.status eq VerdictStatus.CORRECT) or (it.status eq VerdictStatus.WRONG) }.toList()
            }
            val firstCorrect = verdicts.indexOfFirst { it.status == VerdictStatus.CORRECT }
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

        return this.lastScores
    }

    override fun teamScoreMap(): Map<TeamId, Double> = this.lastScoresLock.read { this.lastScores }

    override fun scores(): List<ScoreEntry> = this.lastScoresLock.read {
        this.lastScores.map { ScoreEntry(it.key, null, it.value) }
    }
}