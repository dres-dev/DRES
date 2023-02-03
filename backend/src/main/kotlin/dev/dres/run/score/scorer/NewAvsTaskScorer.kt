package dev.dres.run.score.scorer


import dev.dres.data.model.submissions.DbSubmission
import dev.dres.data.model.submissions.DbVerdictStatus
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.TaskContext
import kotlinx.dnq.query.asSequence
import java.lang.Double.max
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.abs

/**
 * The new AVS Scorer.
 */
class NewAvsTaskScorer(private val penaltyConstant: Double, private val maxPointsPerTask: Double) : TaskScorer {

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
        submissions: Sequence<DbSubmission>,
        context: TaskContext
    ): Map<TeamId, Double> {

        val distinctCorrectVideos = submissions.flatMap { submission ->
            submission.verdicts.asSequence().filter { it.status == DbVerdictStatus.CORRECT && it.item != null }
        }.mapNotNullTo(mutableSetOf()) { it.item }
            .size


        //no correct submissions yet
        if (distinctCorrectVideos == 0) {
            lastScores = this.lastScoresLock.write {
                teamScoreMapSanitised(mapOf(), context.teamIds)
            }
            this.lastScoresLock.read {
                return lastScores
            }
        }

        return teamScoreMapSanitised(submissions.groupBy { it.team }.map { submissionsPerTeam ->
            val verdicts = submissionsPerTeam.value.sortedBy { it.timestamp }.flatMap {
                it.verdicts.asSequence()
                    .filter { v -> v.item != null && (v.status == DbVerdictStatus.CORRECT || v.status == DbVerdictStatus.WRONG) }
            }
            submissionsPerTeam.key.teamId to
                    max(0.0, //prevent negative total scores
                        verdicts.groupBy { it.item!! }.map {
                            val firstCorrectIdx = it.value.indexOfFirst { v -> v.status == DbVerdictStatus.CORRECT }
                            if (firstCorrectIdx < 0) { //no correct submissions, only penalty
                                it.value.size * -penaltyConstant
                            } else {  //apply penalty for everything before correct submission
                                1.0 - firstCorrectIdx * penaltyConstant
                            }
                        }.sum() / distinctCorrectVideos * maxPointsPerTask //normalize
                    )
        }.toMap(), context.teamIds)


//        lastScores = this.lastScoresLock.write {
//            teamScoreMapSanitised(
//                submissions.filter {
//                    it.verdicts.asSequence().any { it.type == VerdictType.TEMPORAL && (it.status == VerdictStatus.CORRECT || it.status == VerdictStatus.WRONG) }
//                }
//
//                    .groupBy { it.teamId }
//                    .map { submissionsPerTeam ->
//                        submissionsPerTeam.key to
//                                max(0.0, //prevent negative total scores
//                                    submissionsPerTeam.value.groupBy { submission ->
//                                        submission as ItemAspect
//                                        submission.item.id
//                                    }.map {
//                                        val firstCorrectIdx = it.value.sortedBy { s -> s.timestamp }
//                                            .indexOfFirst { s -> s.status == SubmissionStatus.CORRECT }
//                                        if (firstCorrectIdx < 0) { //no correct submissions, only penalty
//                                            it.value.size * -penaltyConstant
//                                        } else { //apply penalty for everything before correct submission
//                                            1.0 - firstCorrectIdx * penaltyConstant
//                                        }
//                                    }.sum() / distinctCorrectVideos * maxPointsPerTask //normalize
//                                )
//                    }.toMap(), context.teamIds
//            )
//        }

    }


}
