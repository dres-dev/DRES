package dev.dres.data.model.run

import dev.dres.data.model.run.interfaces.Evaluation
import dev.dres.data.model.submissions.AnswerSet
import dev.dres.data.model.template.task.TaskTemplate

typealias TaskId = String

interface Task { //TODO
    val taskId: TaskId
    val started: Long?
    val ended: Long?
    val evaluation: Evaluation
    val template: TaskTemplate

    fun answerSets(): Sequence<AnswerSet>
}