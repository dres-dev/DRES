package dev.dres.data.model.run

import dev.dres.data.model.UID
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.competition.TaskDescriptionTarget
import dev.dres.data.model.competition.TaskType
import dev.dres.data.model.run.InteractiveSynchronousCompetitionRun.TaskRun
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.score.interfaces.TaskRunScorer
import dev.dres.run.validation.MediaItemsSubmissionValidator
import dev.dres.run.validation.TemporalOverlapSubmissionValidator
import dev.dres.run.validation.interfaces.SubmissionBatchValidator
import dev.dres.run.validation.interfaces.SubmissionValidator
import dev.dres.run.validation.judged.BasicJudgementValidator
import dev.dres.run.validation.judged.BasicVoteValidator
import dev.dres.run.validation.judged.ItemRange
import dev.dres.utilities.TimeUtil

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

    /** Timestamp of when this [TaskRun] was started. */
    @Volatile
    var started: Long? = null
    internal set

    /** Timestamp of when this [TaskRun] was ended. */
    @Volatile
    var ended: Long? = null
    internal set

    /** The [SubmissionFilter] used to filter [Submission]s. */
    @Transient
    val filter: SubmissionFilter = taskDescription.newFilter()

    /** The [SubmissionValidator] used to validate [Submission]s. */
    @Transient
    val validator: SubmissionValidator = newValidator()

    /**
     * Generates and returns a new [SubmissionValidator] for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [SubmissionValidator].
     */
    internal fun newValidator(): SubmissionValidator = when(taskDescription.taskType.targetType.option){
        TaskType.TargetType.SINGLE_MEDIA_ITEM -> MediaItemsSubmissionValidator(setOf((taskDescription.target as TaskDescriptionTarget.MediaItemTarget).item))
        TaskType.TargetType.SINGLE_MEDIA_SEGMENT -> TemporalOverlapSubmissionValidator(taskDescription.target as TaskDescriptionTarget.VideoSegmentTarget)
        TaskType.TargetType.MULTIPLE_MEDIA_ITEMS -> MediaItemsSubmissionValidator((taskDescription.target as TaskDescriptionTarget.MultipleMediaItemTarget).items.toSet())
        TaskType.TargetType.JUDGEMENT -> BasicJudgementValidator(knownCorrectRanges =
        (taskDescription.target as TaskDescriptionTarget.JudgementTaskDescriptionTarget).targets.map {
            if (it.second == null){
                ItemRange(it.first)
            } else {
                val item = it.first
                val range = if (item is MediaItem.VideoItem) {
                    TimeUtil.toMilliseconds(it.second!!, item.fps)
                } else {
                    TimeUtil.toMilliseconds(it.second!!)
                }
                ItemRange(item, range.first, range.second)
            } })
        TaskType.TargetType.VOTE -> BasicVoteValidator(
            knownCorrectRanges =
            (taskDescription.target as TaskDescriptionTarget.VoteTaskDescriptionTarget).targets.map {
                if (it.second == null){
                    ItemRange(it.first)
                } else {
                    val item = it.first
                    val range = if (item is MediaItem.VideoItem) {
                        TimeUtil.toMilliseconds(it.second!!, item.fps)
                    } else {
                        TimeUtil.toMilliseconds(it.second!!)
                    }
                    ItemRange(item, range.first, range.second)
                } },
            parameters = taskDescription.taskType.targetType.parameters

        )
    }

    abstract fun addSubmission(submission: Submission)
}

abstract class NonInteractiveTask : Task() {

    @Transient
    val validator: SubmissionBatchValidator = newValidator()

    internal fun newValidator(): SubmissionBatchValidator = when(taskDescription.taskType.targetType.option){
        TaskType.TargetType.SINGLE_MEDIA_ITEM -> TODO()
        TaskType.TargetType.SINGLE_MEDIA_SEGMENT -> TODO()
        TaskType.TargetType.MULTIPLE_MEDIA_ITEMS -> TODO()
        TaskType.TargetType.JUDGEMENT -> TODO()
        TaskType.TargetType.VOTE -> TODO()
    }
    
    abstract fun addSubmissionBatch(origin: OriginAspect, batches: List<ResultBatch<*>>)
}