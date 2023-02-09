package dev.dres.data.model.submissions

import dev.dres.data.model.run.Task
import dev.dres.data.model.template.interfaces.EvaluationTemplate

interface AnswerSet { //TODO


    val task: Task
    val submission: Submission

    fun answers() : Sequence<Answer>

    fun status() : VerdictStatus
    fun status(status: VerdictStatus)
}