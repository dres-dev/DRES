package dev.dres.run.score.scorer


import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.Scoreable
import java.lang.Double.max
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.abs

/**
 * The new AVS Scorer.
 */
class AvsTaskScorer(override val scoreable: Scoreable, private val penaltyConstant: Double, private val maxPointsPerTask: Double) : TaskScorer {

    private var lastScores: Map<TeamId, Double> = emptyMap()
    private val lastScoresLock = ReentrantReadWriteLock()


    constructor(context: Scoreable, parameters: Map<String, String>) : this(
        context,
        abs(parameters.getOrDefault("penalty", "$defaultPenalty").toDoubleOrNull() ?: defaultPenalty),
        parameters.getOrDefault("maxPointsPerTask", "$defaultMaxPointsPerTask").toDoubleOrNull() ?: defaultMaxPointsPerTask
    )

    constructor(context: Scoreable) : this(context, defaultPenalty, defaultMaxPointsPerTask)

    companion object {
        const val defaultPenalty: Double = 0.2
        private const val defaultMaxPointsPerTask: Double = 1000.0

        /**
         * Sanitised team scores: Either the team has score 0.0 (no submission) or the calculated score
         */
        fun teamScoreMapSanitised(scores: Map<TeamId, Double>, teamIds: Collection<TeamId>): Map<TeamId, Double> {

            val cleanMap = teamIds.associateWith { 0.0 }.toMutableMap()

            scores.forEach { (teamId, score) ->
                cleanMap[teamId] = max(0.0, score)
            }

            return cleanMap
        }

    }

    /**
     * Computes and returns the scores for this [AvsTaskScorer]. Requires an ongoing database transaction.
     *
     * @return A [Map] of [TeamId] to calculated task score.
     */
    override fun scoreMap(submissions: Sequence<Submission>): Map<TeamId, Double> {
        /* Make necessary calculations. */
        val distinctCorrectVideos = submissions.flatMap { submission ->
            submission.answerSets().filter { it.status() == VerdictStatus.CORRECT && it.answers().firstOrNull()?.item != null }
        }.mapNotNullTo(mutableSetOf()) { it.answers().firstOrNull()?.item }
            .size

        //no correct submissions yet
        if (distinctCorrectVideos == 0) {
            lastScores = this.lastScoresLock.write {
                teamScoreMapSanitised(mapOf(), this.scoreable.teams)
            }
            this.lastScoresLock.read {
                return lastScores
            }
        }

        return teamScoreMapSanitised(submissions.groupBy { it.teamId }.map { submissionsPerTeam ->
            val verdicts = submissionsPerTeam.value.sortedBy { it.timestamp }.flatMap {
                it.answerSets()
                    .filter { v -> v.answers().firstOrNull()?.item != null && (v.status() == VerdictStatus.CORRECT || v.status() == VerdictStatus.WRONG) }
            }
            submissionsPerTeam.key to
                    max(0.0, //prevent negative total scores
                        verdicts.groupBy { it.answers().firstOrNull()?.item!! }.map {
                            val firstCorrectIdx = it.value.indexOfFirst { v -> v.status() == VerdictStatus.CORRECT }
                            if (firstCorrectIdx < 0) { //no correct submissions, only penalty
                                it.value.size * -penaltyConstant
                            } else {  //apply penalty for everything before correct submission
                                1.0 - firstCorrectIdx * penaltyConstant
                            }
                        }.sum() / distinctCorrectVideos * maxPointsPerTask //normalize
                    )
        }.toMap(), this.scoreable.teams)
    }
}
