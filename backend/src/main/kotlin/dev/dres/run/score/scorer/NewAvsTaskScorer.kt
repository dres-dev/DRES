package dev.dres.run.score.scorer

import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.ItemAspect
import dev.dres.run.score.ScoreEntry
import dev.dres.run.score.TaskContext
import dev.dres.run.score.interfaces.RecalculatingSubmissionTaskScorer
import dev.dres.run.score.interfaces.TeamTaskScorer
import java.lang.Double.max
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.abs

/**
 * The new AVS Scorer.
 */
class NewAvsTaskScorer(private val penaltyConstant: Double, private val maxPointsPerTask: Double) :
    RecalculatingSubmissionTaskScorer,
    TeamTaskScorer {

    private var lastScores: Map<TeamId, Double> = emptyMap()
    private val lastScoresLock = ReentrantReadWriteLock()


    constructor(parameters: Map<String, String>) : this(
        abs(
            parameters.getOrDefault("penalty", "$defaultPenalty").toDoubleOrNull() ?: defaultPenalty
        ),
        parameters.getOrDefault("maxPointsPerTask", "$defaultMaxPointsPerTask").toDoubleOrNull()
            ?: defaultMaxPointsPerTask
    )

    constructor() : this(defaultPenalty, defaultMaxPointsPerTask)

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

    override fun computeScores(
        submissions: Collection<Submission>,
        context: TaskContext
    ): Map<TeamId, Double> {

        val distinctCorrectVideos =
            submissions.mapNotNullTo(mutableSetOf()) {//map directly to set and filter in one step
                if (it !is ItemAspect || it.status != SubmissionStatus.CORRECT) {
                    null//filter all incorrect submissions
                } else {
                    it.item.id
                }
            }.size

        //no correct submissions yet
        if (distinctCorrectVideos == 0) {
            return teamScoreMapSanitised(mapOf(), context.teamIds)
        }

        lastScores = this.lastScoresLock.write {
            submissions.filter {
                it is ItemAspect &&
                    (it.status == SubmissionStatus.CORRECT || it.status == SubmissionStatus.WRONG)
            }.groupBy { it.teamId }
                .map { submissionsPerTeam ->
                    submissionsPerTeam.key to
                            max(0.0, //prevent negative total scores
                                submissionsPerTeam.value.groupBy { submission ->
                                    submission as ItemAspect
                                    submission.item.id
                                }.map {
                                    val firstCorrectIdx = it.value.sortedBy { s -> s.timestamp }
                                        .indexOfFirst { s -> s.status == SubmissionStatus.CORRECT }
                                    if (firstCorrectIdx < 0) { //no correct submissions, only penalty
                                        it.value.size * -penaltyConstant
                                    } else { //apply penalty for everything before correct submission
                                        1.0 - firstCorrectIdx * penaltyConstant
                                    }
                                }.sum() / distinctCorrectVideos * maxPointsPerTask //normalize
                            )
                }.toMap()
        }

        return this.lastScoresLock.read{
            teamScoreMapSanitised(lastScores, context.teamIds)
        }
    }

    override fun teamScoreMap(): Map<TeamId, Double> = this.lastScoresLock.read { this.lastScores }

    override fun scores(): List<ScoreEntry> = this.lastScoresLock.read {
        this.lastScores.map { ScoreEntry(it.key, null, it.value) }
    }

}
