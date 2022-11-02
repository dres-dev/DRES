package dev.dres.data.model.run

import dev.dres.data.model.competition.task.options.TargetOption
import dev.dres.data.model.submissions.aspects.OriginAspect
import dev.dres.data.model.submissions.batch.ResultBatch
import dev.dres.run.validation.MediaItemsSubmissionBatchValidator
import dev.dres.run.validation.TemporalOverlapSubmissionBatchValidator
import dev.dres.run.validation.TransientMediaSegment
import dev.dres.run.validation.interfaces.SubmissionBatchValidator
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
    fun newValidator(): SubmissionBatchValidator = when(this.description.taskGroup.type.target){
        TargetOption.MEDIA_ITEM -> MediaItemsSubmissionBatchValidator(this.description.targets.mapDistinct { it.item }.filter { it ne null }.toSet())
        TargetOption.MEDIA_SEGMENT -> {
            val target = this.description.targets.filter { (it.item ne null) and (it.start ne null) and (it.end ne null)}.take(1) .first()
            TemporalOverlapSubmissionBatchValidator(TransientMediaSegment(target.item!!, target.range!!))
        }
        TargetOption.JUDGEMENT -> TODO()
        TargetOption.VOTE -> TODO()
        TargetOption.TEXT -> TODO()
        else -> throw IllegalStateException("The provided target option ${this.description.taskGroup.type.target.description} is not supported by non-interactive tasks.")
    }

    /**
     *
     */
    abstract fun addSubmissionBatch(origin: OriginAspect, batches: List<ResultBatch<*>>)
}