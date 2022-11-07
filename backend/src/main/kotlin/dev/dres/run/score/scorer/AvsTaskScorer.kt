package dev.dres.run.score.scorer

import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.media.MediaType
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.run.score.TaskContext
import dev.dres.run.score.interfaces.RecalculatingSubmissionTaskScorer
import dev.dres.run.score.interfaces.ScoreEntry
import dev.dres.run.score.interfaces.TeamTaskScorer
import dev.dres.utilities.TimeUtil
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * A [TeamTaskScorer] used for AVS tasks.
 *
 * @author Luca Rossetto
 * @version 1.0.1
 */
class AvsTaskScorer: RecalculatingSubmissionTaskScorer, TeamTaskScorer {

    private var lastScores: Map<TeamId, Double> = emptyMap()
    private val lastScoresLock = ReentrantReadWriteLock()

    override fun computeScores(submissions: Collection<Submission>, context: TaskContext): Map<TeamId, Double> {
        val correctSubmissions = submissions.filter { it.status == VerdictStatus.CORRECT }
        val wrongSubmissions = submissions.filter { it.status == VerdictStatus.WRONG }

        val correctSubmissionsPerTeam = correctSubmissions.groupBy { it.teamId }
        val wrongSubmissionsPerTeam = wrongSubmissions.groupBy { it.teamId }

        val totalCorrectQuantized = countQuantized(correctSubmissions).toDouble()

        lastScores = this.lastScoresLock.write {
            context.teamIds.map { teamid ->

                val correctSubs = correctSubmissionsPerTeam[teamid] ?: return@map teamid to 0.0

                val correct = correctSubs.size

                val wrong = wrongSubmissionsPerTeam[teamid]?.size ?: 0

                teamid to 100.0 *
                        (correct / (correct + wrong / 2.0)) *
                        (countQuantized(correctSubs).toDouble() / totalCorrectQuantized)
            }.toMap()
        }
        return teamScoreMap()
    }

    private fun countQuantized(submissions: Collection<Submission>): Int = submissions
        .filter { it.item != null }
        .groupBy { it.item }.map {
            when(it.key!!.type) {
                MediaType.IMAGE -> 1
                MediaType.VIDEO -> {
                    val ranges = it.value.map { s -> s.temporalRange }
                    TimeUtil.merge(ranges, overlap = 1).size
                }
                else -> throw IllegalStateException("Unsupported media type ${it.key!!.type} for AVS task scorer.")
            }
        }.sum()

    override fun teamScoreMap(): Map<TeamId, Double> = this.lastScoresLock.read { this.lastScores }

    override fun scores(): List<ScoreEntry> = this.lastScoresLock.read {
        this.lastScores.map { ScoreEntry(it.key, null, it.value) }
    }
}