package dev.dres.data.model.run

import dev.dres.data.model.competition.TaskDescriptionTarget
import dev.dres.data.model.competition.options.TargetOption
import dev.dres.data.model.run.interfaces.Task
import dev.dres.data.model.submissions.aspects.OriginAspect
import dev.dres.data.model.submissions.batch.ResultBatch
import dev.dres.run.validation.MediaItemsSubmissionBatchValidator
import dev.dres.run.validation.TemporalOverlapSubmissionBatchValidator
import dev.dres.run.validation.interfaces.SubmissionBatchValidator

/**
 * An abstract [Task] implementation for non-interactive [Task], i.e., [Task]s that do not rely on human interaction and simply process input data in batches
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractNonInteractiveTask: AbstractTaskRun(), Task {

    @Transient
    val validator: SubmissionBatchValidator = newValidator()

    internal fun newValidator(): SubmissionBatchValidator = when(this.description.taskType.targetType.option){
        TargetOption.SINGLE_MEDIA_ITEM -> MediaItemsSubmissionBatchValidator(setOf((description.target as TaskDescriptionTarget.MediaItemTarget).item))
        TargetOption.SINGLE_MEDIA_SEGMENT -> TemporalOverlapSubmissionBatchValidator(description.target as TaskDescriptionTarget.VideoSegmentTarget)
        TargetOption.MULTIPLE_MEDIA_ITEMS -> MediaItemsSubmissionBatchValidator((description.target as TaskDescriptionTarget.MultipleMediaItemTarget).items.toSet())
        TargetOption.JUDGEMENT -> TODO()
        TargetOption.VOTE -> TODO()
        TargetOption.TEXT -> TODO()
    }

    abstract fun addSubmissionBatch(origin: OriginAspect, batches: List<ResultBatch<*>>)
}