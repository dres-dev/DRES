package dev.dres.data.model.run

import dev.dres.data.model.competition.TaskType
import dev.dres.data.model.run.interfaces.Task
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.aspects.OriginAspect
import dev.dres.data.model.submissions.batch.ResultBatch
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
        TaskType.TargetType.SINGLE_MEDIA_ITEM -> TODO()
        TaskType.TargetType.SINGLE_MEDIA_SEGMENT -> TODO()
        TaskType.TargetType.MULTIPLE_MEDIA_ITEMS -> TODO()
        TaskType.TargetType.JUDGEMENT -> TODO()
        TaskType.TargetType.VOTE -> TODO()
    }

    abstract fun addSubmissionBatch(origin: OriginAspect, batches: List<ResultBatch<*>>)
}