package dev.dres.run.eventstream.handlers

import dev.dres.data.model.UID
import dev.dres.data.model.run.Submission
import dev.dres.data.model.run.SubmissionStatus
import dev.dres.run.eventstream.*

class SubmissionStatisticsHandler : StreamEventHandler {

    private val submissionTaskMap = mutableMapOf<String, MutableList<Submission>>()
    private val taskStartMap = mutableMapOf<String, Long>()

    override fun handle(event: StreamEvent) {
        when (event) {
            is TaskStartEvent -> {
                submissionTaskMap[event.taskId] = mutableListOf()
                taskStartMap[event.taskId] = event.timeStamp
            }
            is SubmissionEvent -> if (event.taskId != null && taskStartMap.containsKey(event.taskId)){
                submissionTaskMap[event.taskId]!!.add(event.submission)
            }
            is TaskEndEvent -> {
                computeStatistics(submissionTaskMap[event.taskId]!!, taskStartMap[event.taskId]!!)
                submissionTaskMap.remove(event.taskId)
            }
            else -> {/* ignore */
            }
        }
    }

    private fun computeStatistics(submissions: List<Submission>, taskStart: Long) {

        val submissionsByTeam = submissions.groupBy { it.team }

        val totalSubmissionsPerTeam = submissionsByTeam.mapValues { it.value.size }
        val timeUntilCorrectSubmission = submissionsByTeam.mapValues {
            it.value.firstOrNull { s -> s.status == SubmissionStatus.CORRECT }?.timestamp?.minus(taskStart) }
                .filter { it.value != null }
        val incorrectBeforeCorrectSubmissions = submissionsByTeam.mapValues {
            it.value.indexOfFirst { s -> s.status == SubmissionStatus.CORRECT } }


    }


}