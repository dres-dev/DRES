package dev.dres.run.score.scorer

import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.data.model.run.TemporalSubmissionAspect
import dev.dres.run.score.interfaces.RecalculatingTaskRunScorer
import dev.dres.utilities.TimeUtil
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read

class AvsTaskScorer: RecalculatingTaskRunScorer {

    private var lastScores: Map<Int, Double> = emptyMap()
    private val lastScoresLock = ReentrantReadWriteLock()

    override fun computeScores(submissions: Collection<Submission>, teamIds: Collection<Int>, taskStartTime: Long, taskDuration: Long, taskEndTime: Long): Map<Int, Double> {

        val correctSubmissions = submissions.filter { it.status == SubmissionStatus.CORRECT }
        val wrongSubmissions = submissions.filter { it.status == SubmissionStatus.WRONG }

        val correctSubmissionsPerTeam = correctSubmissions.groupBy { it.team }
        val wrongSubmissionsPerTeam = wrongSubmissions.groupBy { it.team }

        val temporal = correctSubmissions.all { it is TemporalSubmissionAspect }

        lastScores = if (temporal){

            val ranges = submissions.groupBy { it.item.id }.map { (item, submissions) ->
                item to TimeUtil.merge(submissions.map { (it as TemporalSubmissionAspect).temporalRange }, overlap = 1000)
            }.toMap()

            teamIds.map { team ->
                val correct = correctSubmissionsPerTeam[team]?.size ?: return@map team to 0.0
                val wrong = wrongSubmissionsPerTeam[team]?.size ?: 0

                val teamRanges = correctSubmissionsPerTeam[team]!!.groupBy { it.item }.map {(item, subs) ->
                    val rangesInItem = ranges[item.id]!!

                    subs.map {
                        val tr = (it as TemporalSubmissionAspect).temporalRange
                        rangesInItem.find { it.contains(tr) }
                    }.toSet().size
                }.sum()

                return@map team to 50.0 * ((correct / (correct + wrong / 2.0)) + (teamRanges / ranges.size))

            }

        } else {
            teamIds.map { team ->
                val correct = correctSubmissionsPerTeam[team]?.size ?: return@map team to 0.0
                val wrong = wrongSubmissionsPerTeam[team]?.size ?: 0
                return@map team to 100.0 * (correct / (correct + wrong / 2.0))
            }
        }.toMap()

        return lastScores
    }

    override fun scores(): Map<Int, Double> = this.lastScoresLock.read { this.lastScores }

}