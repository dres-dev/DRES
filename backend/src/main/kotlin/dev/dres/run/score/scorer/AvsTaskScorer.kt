package dev.dres.run.score.scorer

import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.media.DbMediaType
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbAnswerSet
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.run.score.TaskContext
import dev.dres.utilities.TimeUtil
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.toList


/**
 * A [TeamTaskScorer] used for AVS tasks.
 *
 * @author Luca Rossetto
 * @version 1.1.0
 */
object AvsTaskScorer : TaskScorer {

    /**
     * TODO: Check for correctness especially if a submission has more than one verdict. Maybe add sanity checks.
     */
    override fun computeScores(submissions: Sequence<DbSubmission>, context: TaskContext): Map<TeamId, Double> {
        val correctSubmissions =
            submissions.flatMap { s -> s.verdicts.filter { v -> v.status eq DbVerdictStatus.CORRECT }.asSequence() }
        val wrongSubmissions =
            submissions.flatMap { s -> s.verdicts.filter { v -> v.status eq DbVerdictStatus.WRONG }.asSequence() }
        val correctSubmissionsPerTeam = correctSubmissions.groupBy { it.submission.team.id }
        val wrongSubmissionsPerTeam = wrongSubmissions.groupBy { it.submission.team.id }
        val totalCorrectQuantized = countQuantized(correctSubmissions).toDouble()

        return context.teamIds.map { teamid ->
            val correctSubs = correctSubmissionsPerTeam[teamid] ?: return@map teamid to 0.0
            val correct = correctSubs.size
            val wrong = wrongSubmissionsPerTeam[teamid]?.size ?: 0
            teamid to 100.0 * (correct / (correct + wrong / 2.0)) * (countQuantized(correctSubs.asSequence()).toDouble() / totalCorrectQuantized)
        }.toMap()

    }

    private fun countQuantized(submissions: Sequence<DbAnswerSet>): Int = submissions
        .filter { it.item != null }
        .groupBy { it.item }
        .map {
            when (it.key!!.type) {
                DbMediaType.IMAGE -> 1
                DbMediaType.VIDEO -> {
                    val ranges = it.value.map { s -> s.temporalRange!! }
                    TimeUtil.merge(ranges, overlap = 1).size
                }
                else -> throw IllegalStateException("Unsupported media type ${it.key!!.type} for AVS task scorer.")
            }
        }.sum()


}
