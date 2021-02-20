package dev.dres.data.model.run

import dev.dres.data.model.UID
import dev.dres.data.model.competition.TaskDescription
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.score.interfaces.TaskRunScorer
import dev.dres.run.validation.interfaces.SubmissionBatchValidator
import dev.dres.run.validation.interfaces.SubmissionValidator

typealias TaskId = UID

abstract class Task {

    abstract val uid: TaskId

    /** Reference to the [TaskDescription] describing this [Task]. */
    abstract val taskDescription: TaskDescription

    /** The [TaskRunScorer] used to update score for this [Task]. */
    @Transient
    val scorer: TaskRunScorer = taskDescription.newScorer()

}

abstract class InteractiveTask : Task() {

    abstract var started: Long?
        internal set

    abstract var ended: Long?
        internal set

    /** The [SubmissionFilter] used to filter [Submission]s. */
    @Transient
    val filter: SubmissionFilter = taskDescription.newFilter()

    /** The [SubmissionValidator] used to validate [Submission]s. */
    @Transient
    val validator: SubmissionValidator = taskDescription.newValidator()

    abstract fun addSubmission(submission: Submission)
}

abstract class NonInteractiveTask : Task() {

    /**
     * @return true if results were added to the task
     */
    fun  addSubmissionBatch(batch: BaseSubmissionBatch<*>) : Boolean {
        val batches = batch.results.filter { it.task == this.uid }
        if (batches.isNotEmpty()){
            addSubmissionBatch(batch, batches)
            return true
        }
        return false
    }

    abstract val validator: SubmissionBatchValidator
    
    abstract fun addSubmissionBatch(origin: OriginAspect, batches: List<BaseBatch<*>>)
}