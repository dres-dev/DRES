package dev.dres.data.model.run

import dev.dres.data.model.competition.TaskDescriptionTarget
import dev.dres.data.model.competition.TaskType
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
abstract class AbstractNonInteractiveTask: AbstractRun(), Task {

    @Transient
    val validator: SubmissionBatchValidator = newValidator()

    internal fun newValidator(): SubmissionBatchValidator = when(this.description.taskType.targetType.option){
        TaskType.TargetType.SINGLE_MEDIA_ITEM -> MediaItemsSubmissionBatchValidator(setOf((description.target as TaskDescriptionTarget.MediaItemTarget).item))
        TaskType.TargetType.SINGLE_MEDIA_SEGMENT -> TemporalOverlapSubmissionBatchValidator(description.target as TaskDescriptionTarget.VideoSegmentTarget)
        TaskType.TargetType.MULTIPLE_MEDIA_ITEMS -> MediaItemsSubmissionBatchValidator((description.target as TaskDescriptionTarget.MultipleMediaItemTarget).items.toSet())
        TaskType.TargetType.JUDGEMENT -> TODO()
        TaskType.TargetType.VOTE -> TODO()
    }

    abstract fun addSubmissionBatch(origin: OriginAspect, batches: List<ResultBatch<*>>)
}