package dres.run.score.scorer

import dres.data.model.basics.TemporalRange
import dres.data.model.run.Submission
import dres.data.model.run.SubmissionStatus
import dres.run.score.interfaces.IncrementalTaskRunScorer
import dres.utilities.TimeUtil

//TODO this implicitly assumes that all submissions are from the same task, there is no way to check this here
//TODO this assumes that the status of a submission never changes outside of a call to update and that a submission which was validated as CORRECT or WRONG does not change
class AvsTaskScorer: IncrementalTaskRunScorer {

    private val correctSubmissions = mutableSetOf<Submission>()
    private val correctSubmissionsPerTeam = mutableMapOf<Int, MutableSet<Submission>>()

    private val ranges = mutableMapOf<String, List<TemporalRange>>()

    private val wrongSubmissions = mutableSetOf<Submission>()
    private val wrongSubmissionsPerTeam = mutableMapOf<Int, MutableSet<Submission>>()

    private fun addToMap(map: MutableMap<Int, MutableSet<Submission>>, submission: Submission) {
        if (!map.containsKey(submission.team)) {
            map[submission.team] = mutableSetOf(submission)
        } else {
            map[submission.team]!!.add(submission)
        }
    }

    override fun update(submission: Submission) {

        //TODO the following assumes that submissions have a start and an end, figure out what to do in case they don't

        when(submission.status){
            SubmissionStatus.CORRECT -> {
                correctSubmissions.add(submission)
                addToMap(correctSubmissionsPerTeam, submission)

                //update quantization
                if (!ranges.containsKey(submission.item)){
                    ranges[submission.item] = listOf(submission.temporalRange())
                }else{
                    val merged = TimeUtil.merge(ranges[submission.item]!!.plusElement(submission.temporalRange()))
                    ranges[submission.item] = merged
                }

            }
            SubmissionStatus.WRONG -> {
                wrongSubmissions.add(submission)
                addToMap(wrongSubmissionsPerTeam, submission)
            }
            else -> {
                //TODO this branch should not be reached, log in case it does
                return
            }
        }

    }

    private fun countRanges(submissions: Collection<Submission>): Int {

        return submissions.groupBy { it.item }.map {(item, subs) ->
            val rangesInItem = ranges[item]!!

            subs.map {
                val tr = it.temporalRange()
                rangesInItem.find { it.contains(tr) }
            }.toSet().size

        }.sum()

    }


    override fun scores(): Map<Int, Double> {

        val teams = correctSubmissionsPerTeam.keys.plus(wrongSubmissionsPerTeam.keys).toSet()

        val allRanges = ranges.values.map { it.size }.sum()

        return teams.map { team ->
            val correct = correctSubmissionsPerTeam[team]?.size ?: 0

            if (correct == 0){
                return@map team to 0.0
            }

            val wrong = wrongSubmissionsPerTeam[team]?.size ?: 0

            return@map team to (100.0 * correct / (correct + wrong / 2.0)) + (countRanges(correctSubmissionsPerTeam[team]!!) / allRanges)
        }.toMap()

    }


}