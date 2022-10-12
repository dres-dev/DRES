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
class AvsTaskScorer(private val penaltyConstant: Double) : RecalculatingSubmissionTaskScorer,
    TeamTaskScorer {

    private var lastScores: Map<TeamId, Double> = emptyMap()
    private val lastScoresLock = ReentrantReadWriteLock()


    constructor(parameters: Map<String, String>) : this(
        parameters.getOrDefault("submissionWindow", "$defaultSubmissionWindow").toDoubleOrNull()
            ?: defaultSubmissionWindow
    )

    companion object {
        const val defaultSubmissionWindow: Double = 5.0
    }

    override fun computeScores(
        submissions: Collection<Submission>,
        context: TaskContext
    ): Map<TeamId, Double> {
        val distinctVideos = mutableSetOf<UID>()
        lastScores = this.lastScoresLock.write {
            submissions.filter { it is ItemAspect }.groupBy { it.teamId }.map { subsPerTeam ->
                subsPerTeam.key to
                        subsPerTeam.value.groupBy { sub ->
                            sub as ItemAspect
                            sub.item
                        }.map {
                            val firstCorrectIdx = it.value.sortedBy { it.timestamp }
                                .indexOfFirst { it.status == SubmissionStatus.CORRECT }

                            val c = if (firstCorrectIdx == -1
                            ) {
                                0.0
                            } else {
                                distinctVideos.add(it.key.id)
                                1.0
                            }
                            c - firstCorrectIdx * penaltyConstant
                        }.sum()
            }.toMap().mapValues { it.value / distinctVideos.size }

        }

        return teamScoreMap()
    }

    override fun teamScoreMap(): Map<TeamId, Double> = this.lastScoresLock.read { this.lastScores }

    override fun scores(): List<ScoreEntry> = this.lastScoresLock.read {
        this.lastScores.map { ScoreEntry(it.key, null, it.value) }
    }
}
