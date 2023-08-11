package dev.dres.run.score.scorer

import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.media.MediaItemType
import dev.dres.data.model.media.time.TemporalRange
import dev.dres.data.model.submissions.*
import dev.dres.run.score.Scoreable
import jetbrains.exodus.database.TransientEntityStore

/**
 * A [TeamTaskScorer] used for AVS tasks.
 *
 * @author Luca Rossetto
 * @version 2.0.0
 */
class LegacyAvsTaskScorer(scoreable: Scoreable, store: TransientEntityStore?) : AbstractTaskScorer(scoreable, store) {

    /**
     * Computes and returns the scores for this [LegacyAvsTaskScorer] based on a [Sequence] of [Submission]s.
     *
     * The sole use of this method is to keep the implementing classes unit-testable (irrespective of the database).
     *
     * @param submissions A [Sequence] of [Submission]s to obtain scores for.
     * @return A [Map] of [TeamId] to calculated task score.
     */
    override fun calculateScores(submissions: Sequence<Submission>): Map<TeamId, Double>  {
        val correctSubmissions = submissions.flatMap { s -> s.answerSets().filter { v -> v.status() == VerdictStatus.CORRECT } }
        val wrongSubmissions = submissions.flatMap { s -> s.answerSets().filter { v -> v.status() == VerdictStatus.WRONG } }
        val correctSubmissionsPerTeam = correctSubmissions.groupBy { it.submission.teamId }
        val wrongSubmissionsPerTeam = wrongSubmissions.groupBy { it.submission.teamId }
        val totalCorrectQuantized = countQuantized(correctSubmissions).toDouble()

        return this.scoreable.teams.map { teamId ->
            val correctSubs = correctSubmissionsPerTeam[teamId] ?: return@map teamId to 0.0
            val correct = correctSubs.size
            val wrong = wrongSubmissionsPerTeam[teamId]?.size ?: 0
            teamId to 100.0 * (correct / (correct + wrong / 2.0)) * (countQuantized(correctSubs.asSequence()).toDouble() / totalCorrectQuantized)
        }.toMap()
    }

    private fun countQuantized(submissions: Sequence<AnswerSet>): Int = submissions
        .filter { it.answers().firstOrNull()?.item != null }
        .groupBy { it.answers().first().item }
        .map {
            when (it.key!!.type()) {
                MediaItemType.IMAGE -> 1
                MediaItemType.VIDEO -> {
                    val ranges = it.value.asSequence().map { s -> s.answers().first().temporalRange!! }.toList()
                    TemporalRange.merge(ranges, overlap = 1).size
                }
                else -> throw IllegalStateException("Unsupported media type ${it.key!!.type()} for AVS task scorer.")
            }
        }.sum()
}
