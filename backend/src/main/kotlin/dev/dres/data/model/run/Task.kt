package dev.dres.data.model.run

import dev.dres.data.model.submissions.AnswerSet

typealias TaskId = String

interface Task { //TODO

    val taskId: TaskId
    val started: Long?
    val ended: Long?

    fun answerSets(): Sequence<AnswerSet>

}