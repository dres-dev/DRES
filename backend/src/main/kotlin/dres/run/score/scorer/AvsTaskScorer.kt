package dres.run.score.scorer

import dres.data.model.basics.time.TemporalRange
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import dres.run.score.interfaces.IncrementalTaskRunScorer
import dres.utilities.TimeUtil
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

//TODO this implicitly assumes that all submissions are from the same task, there is no way to check this here
//TODO this assumes that the status of a submission never changes outside of a call to update and that a submission which was validated as CORRECT or WRONG does not change
class AvsTaskScorer: IncrementalTaskRunScorer {

    private val correctSubmissions = mutableSetOf<Submission>()
    private val correctSubmissionsPerTeam = mutableMapOf<Int, MutableSet<Submission>>()

    private val ranges = mutableMapOf<Long, List<TemporalRange>>()

    private val wrongSubmissions = mutableSetOf<Submission>()
    private val wrongSubmissionsPerTeam = mutableMapOf<Int, MutableSet<Submission>>()

    private val updateScoresLock = ReentrantReadWriteLock()

    private fun addToMap(map: MutableMap<Int, MutableSet<Submission>>, submission: Submission) {
        if (!map.containsKey(submission.team)) {
            map[submission.team] = mutableSetOf(submission)
        } else {
            map[submission.team]!!.add(submission)
        }
    }

    override fun update(submission: Submission) = updateScoresLock.write{

        //TODO the following assumes that submissions have a start and an end, figure out what to do in case they don't

        when(submission.status){
            SubmissionStatus.CORRECT -> {
                correctSubmissions.add(submission)
                addToMap(correctSubmissionsPerTeam, submission)

                //update quantization
                if (!ranges.containsKey(submission.item.id)){
                    ranges[submission.item.id] = listOf(submission.temporalRange())
                }else{
                    val merged = TimeUtil.merge(ranges[submission.item.id]!!.plusElement(submission.temporalRange()), overlap = 1000)
                    ranges[submission.item.id] = merged
                }

            }
            SubmissionStatus.WRONG -> {
                wrongSubmissions.add(submission)
                addToMap(wrongSubmissionsPerTeam, submission)
            }
            else -> {
                //ignore INDETERMINATE or UNDECIDABLE
            }
        }

    }

    private fun countRanges(submissions: Collection<Submission>): Int  = updateScoresLock.read{

        return submissions.groupBy { it.item }.map {(item, subs) ->
            val rangesInItem = ranges[item.id]!!

            subs.map {
                val tr = it.temporalRange()
                rangesInItem.find { it.contains(tr) }
            }.toSet().size

        }.sum()

    }


    override fun scores(): Map<Int, Double> = updateScoresLock.read{

        val teams = correctSubmissionsPerTeam.keys.plus(wrongSubmissionsPerTeam.keys).toSet()

        val allRanges = ranges.values.map { it.size }.sum()

        return teams.map { team ->
            val correct = correctSubmissionsPerTeam[team]?.size ?: 0

            if (correct == 0){
                return@map team to 0.0
            }

            val wrong = wrongSubmissionsPerTeam[team]?.size ?: 0

            return@map team to 50.0 * ((correct / (correct + wrong / 2.0)) + (countRanges(correctSubmissionsPerTeam[team]!!) / allRanges))
        }.toMap()

    }


}