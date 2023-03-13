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

    /**
     * checks if the answers of a given [AnswerSet] have the same content as
     */
    infix fun equivalent(answerSet: AnswerSet): Boolean {

        if (this.answers().count() != answerSet.answers().count()) {
            return false
        }

        val tmp = this.answers().toMutableList()

        //pairwise comparison
        answerSet.answers().forEach { answer ->
            //this assumes that there are no duplicates within an AnswerSet
            tmp.removeIf { it eq answer }
        }

        return tmp.isEmpty()

    }
}