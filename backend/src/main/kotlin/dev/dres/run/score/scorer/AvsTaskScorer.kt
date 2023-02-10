package dev.dres.run.score.scorer

import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.media.MediaItemType
import dev.dres.data.model.submissions.*
import dev.dres.run.score.TaskContext
import dev.dres.utilities.TimeUtil

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
    override fun computeScores(submissions: Sequence<Submission>, context: TaskContext): Map<TeamId, Double> {
        val correctSubmissions =
            submissions.flatMap { s -> s.answerSets().filter { v -> v.status() == VerdictStatus.CORRECT } }
        val wrongSubmissions =
            submissions.flatMap { s -> s.answerSets().filter { v -> v.status() == VerdictStatus.WRONG } }
        val correctSubmissionsPerTeam = correctSubmissions.groupBy { it.submission.teamId }
        val wrongSubmissionsPerTeam = wrongSubmissions.groupBy { it.submission.teamId }
        val totalCorrectQuantized = countQuantized(correctSubmissions).toDouble()

        return context.teamIds.map { teamid ->
            val correctSubs = correctSubmissionsPerTeam[teamid] ?: return@map teamid to 0.0
            val correct = correctSubs.size
            val wrong = wrongSubmissionsPerTeam[teamid]?.size ?: 0
            teamid to 100.0 * (correct / (correct + wrong / 2.0)) * (countQuantized(correctSubs.asSequence()).toDouble() / totalCorrectQuantized)
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
                    TimeUtil.merge(ranges, overlap = 1).size
                }
                else -> throw IllegalStateException("Unsupported media type ${it.key!!.type()} for AVS task scorer.")
            }
        }.sum()


}
