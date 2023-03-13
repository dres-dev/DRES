package dev.dres.data.model.submissions

import dev.dres.data.model.run.Task
import dev.dres.data.model.run.TaskId

typealias AnswerSetId = String
interface AnswerSet {
    val id : AnswerSetId
    val taskId: TaskId
    val submission: Submission

    fun task(): Task

    fun answers() : Sequence<Answer>

    fun status() : VerdictStatus
    fun status(status: VerdictStatus)
}