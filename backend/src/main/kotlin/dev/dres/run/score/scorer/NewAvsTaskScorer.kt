package dev.dres.run.score.scorer

import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.ItemAspect
import dev.dres.run.score.ScoreEntry
import dev.dres.run.score.TaskContext
import dev.dres.run.score.interfaces.RecalculatingSubmissionTaskScorer
import dev.dres.run.score.interfaces.TeamTaskScorer
import okhttp3.internal.toImmutableMap
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
            return teamScoreMapSanitised(context.teamIds)
        }

        lastScores = this.lastScoresLock.write {
            submissions.filter { it is ItemAspect }.groupBy { it.teamId }
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

        return teamScoreMapSanitised(context.teamIds)
    }

    override fun teamScoreMap(): Map<TeamId, Double> = this.lastScoresLock.read { this.lastScores }

    override fun scores(): List<ScoreEntry> = this.lastScoresLock.read {
        this.lastScores.map { ScoreEntry(it.key, null, it.value) }
    }

    /**
     * Sanitised team scores: Either the team has score 0.0 (no submission) or the calculated score
     */
    private fun teamScoreMapSanitised(teamIds: Collection<TeamId>): Map<TeamId, Double> =
        this.lastScoresLock.read {
            if (this.lastScores.isEmpty()) {
                /* if nothing has been submitted so far, all have score 0 */
                return teamIds.associateWith { 0.0 }
            }
            val scores = this.lastScores.toMutableMap()

            /* Sanitise teams that didn't do a submission (yet) */
            teamIds.forEach {
                if (!scores.containsKey(it)) {
                    scores[it] = 0.0
                }
            }

            return scores.toImmutableMap()
        }
}
