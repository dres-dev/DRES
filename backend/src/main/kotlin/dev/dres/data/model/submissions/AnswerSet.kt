package dev.dres.data.model.submissions

import dev.dres.data.model.run.Task

interface AnswerSet { //TODO

    val status: VerdictStatus
    val task: Task
    val submission: Submission

    fun answers() : Sequence<Answer>
}