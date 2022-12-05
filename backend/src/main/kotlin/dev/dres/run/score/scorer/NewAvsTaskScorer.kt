package dev.dres.run.score.scorer

import dev.dres.data.model.UID
import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.SubmissionStatus
import dev.dres.data.model.submissions.aspects.ItemAspect
import dev.dres.run.score.ScoreEntry
import dev.dres.run.score.TaskContext
import dev.dres.run.score.interfaces.RecalculatingSubmissionTaskScorer
import dev.dres.run.score.interfaces.TeamTaskScorer
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * The new AVS Scorer.
 */
class NewAvsTaskScorer(private val penaltyConstant: Double, private val maxPointsPerTask: Double) : RecalculatingSubmissionTaskScorer,
    TeamTaskScorer {

    private var lastScores: Map<TeamId, Double> = emptyMap()
    private val lastScoresLock = ReentrantReadWriteLock()


    constructor(parameters: Map<String, String>) : this(
        parameters.getOrDefault("penalty", "$defaultPenalty").toDoubleOrNull() ?: defaultPenalty,
        parameters.getOrDefault("maxPointsPerTask", "$defaultMaxPointsPerTask").toDoubleOrNull() ?: defaultMaxPointsPerTask
    )

    companion object {
        const val defaultPenalty: Double = 0.2
        private const val defaultMaxPointsPerTask: Double = 1000.0
    }

    override fun computeScores(
        submissions: Collection<Submission>,
        context: TaskContext
    ): Map<TeamId, Double> {

        val distinctCorrectVideos = submissions.mapNotNullTo(mutableSetOf()) {//map directly to set and filter in one step
            if (it !is ItemAspect || it.status != SubmissionStatus.CORRECT) {
                null//filter all incorrect submissions
            } else {
                it.item.id
            }
        }.size

        //no correct submissions yet
        if (distinctCorrectVideos == 0) {
            return teamScoreMap()
        }

        lastScores = this.lastScoresLock.write {
            submissions.filter { it is ItemAspect }.groupBy { it.teamId }.map { submissionsPerTeam ->
                submissionsPerTeam.key to
                    submissionsPerTeam.value.groupBy { submission ->
                        submission as ItemAspect
                        submission.item.id
                    }.map {
                        val firstCorrectIdx = it.value.sortedBy { s -> s.timestamp }.indexOfFirst { s -> s.status == SubmissionStatus.CORRECT }
                        if (firstCorrectIdx < 0) { //no correct submissions, only penalty
                            it.value.size * penaltyConstant
                        } else { //apply penalty for everything before correct submission
                            1.0 - firstCorrectIdx * penaltyConstant
                        }
                    }.sum() / distinctCorrectVideos * maxPointsPerTask //normalize
            }.toMap()
        }

        return teamScoreMap()
    }

    override fun teamScoreMap(): Map<TeamId, Double> = this.lastScoresLock.read { this.lastScores }

    override fun scores(): List<ScoreEntry> = this.lastScoresLock.read {
        this.lastScores.map { ScoreEntry(it.key, null, it.value) }
    }
}
