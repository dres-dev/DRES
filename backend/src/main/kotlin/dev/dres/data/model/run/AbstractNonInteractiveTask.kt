package dev.dres.data.model.run

import dev.dres.data.model.admin.UserId
import dev.dres.data.model.template.task.options.TargetOption
import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.submissions.batch.ResultBatch
import dev.dres.run.validation.TransientMediaSegment
import kotlinx.dnq.query.*

/**
 * An abstract [Task] implementation for non-interactive [Task], i.e., [Task]s that do not rely on human interaction and simply process input data in batches
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
abstract class AbstractNonInteractiveTask(task: Task): AbstractTaskRun(task) {

    /** The [SubmissionBatchValidator] used by this [AbstractNonInteractiveTask]. */
    @Transient
    val validator: SubmissionBatchValidator = newValidator()

    /**
     * Generates a new [SubmissionBatchValidator].
     *
     * @return [SubmissionBatchValidator]
     */
    fun newValidator(): SubmissionBatchValidator = when(this.template.taskGroup.type.target){
        TargetOption.MEDIA_ITEM -> MediaItemsSubmissionBatchValidator(this.template.targets.mapDistinct { it.item }.filter { it ne null }.toSet())
        TargetOption.MEDIA_SEGMENT -> {
            val target = this.template.targets.filter { (it.item ne null) and (it.start ne null) and (it.end ne null)}.take(1) .first()
            TemporalOverlapSubmissionBatchValidator(TransientMediaSegment(target.item!!, target.range!!))
        }
        TargetOption.JUDGEMENT -> TODO()
        TargetOption.VOTE -> TODO()
        TargetOption.TEXT -> TODO()
        else -> throw IllegalStateException("The provided target option ${this.template.taskGroup.type.target.description} is not supported by non-interactive tasks.")
    }

    /**
     * Submits a batch of [Submissions].
     */
    abstract fun addSubmissionBatch(teamId: TeamId, memberId: UserId, batches: List<ResultBatch<*>>)
}