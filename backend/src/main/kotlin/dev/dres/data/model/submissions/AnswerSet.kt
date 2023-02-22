package dev.dres.data.model.submissions

import dev.dres.data.model.run.Task
import dev.dres.data.model.run.TaskId

interface AnswerSet { //TODO
    val taskId: TaskId
    val submission: Submission

    fun task(): Task

    fun answers() : Sequence<Answer>

    fun status() : VerdictStatus
    fun status(status: VerdictStatus)
}