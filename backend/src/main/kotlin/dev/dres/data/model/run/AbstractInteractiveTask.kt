package dev.dres.data.model.run

import dev.dres.data.model.competition.TaskDescription
import dev.dres.data.model.competition.TaskDescriptionTarget
import dev.dres.data.model.competition.options.TargetOption
import dev.dres.data.model.run.interfaces.Task
import dev.dres.data.model.submissions.Submission
import dev.dres.run.filter.SubmissionFilter
import dev.dres.run.validation.MediaItemsSubmissionValidator
import dev.dres.run.validation.TemporalOverlapSubmissionValidator
import dev.dres.run.validation.interfaces.SubmissionValidator
import dev.dres.run.validation.judged.BasicJudgementValidator
import dev.dres.run.validation.judged.BasicVoteValidator
import dev.dres.run.validation.judged.ItemRange
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * An abstract [Task] implementation for interactive [Task], i.e. [Task]s that rely on human interaction, such as [Submission]s
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractInteractiveTask: AbstractRun(), Task {
    /** List of [Submission]s* registered for this [Task]. */
    val submissions: ConcurrentLinkedQueue<Submission> = ConcurrentLinkedQueue<Submission>()

    /** The total duration in milliseconds of this task. Usually determined by the [TaskDescription] but can be adjusted! */
    abstract var duration: Long

    /** The [SubmissionFilter] used to filter [Submission]s. */
    abstract val filter: SubmissionFilter

    /** The [SubmissionValidator] used to validate [Submission]s. */
    abstract val validator: SubmissionValidator

    /**
     * Generates and returns a new [SubmissionValidator] for this [TaskDescription]. Depending
     * on the implementation, the returned instance is a new instance or being re-use.
     *
     * @return [SubmissionValidator].
     */
    internal fun newValidator(): SubmissionValidator = when(description.taskType.targetType.option){
        TargetOption.SINGLE_MEDIA_ITEM -> MediaItemsSubmissionValidator(setOf((description.target as TaskDescriptionTarget.MediaItemTarget).item))
        TargetOption.SINGLE_MEDIA_SEGMENT -> TemporalOverlapSubmissionValidator(description.target as TaskDescriptionTarget.VideoSegmentTarget)
        TargetOption.MULTIPLE_MEDIA_ITEMS -> MediaItemsSubmissionValidator((description.target as TaskDescriptionTarget.MultipleMediaItemTarget).items.toSet())
        TargetOption.JUDGEMENT -> BasicJudgementValidator(knownCorrectRanges =
        (description.target as TaskDescriptionTarget.JudgementTaskDescriptionTarget).targets.map {
            if (it.second == null) {
                ItemRange(it.first)
            } else {
                val item = it.first
                val range = it.second!!.toMilliseconds()
                ItemRange(item, range.first, range.second)
            } })
        TargetOption.VOTE -> BasicVoteValidator(
            knownCorrectRanges =
            (description.target as TaskDescriptionTarget.VoteTaskDescriptionTarget).targets.map {
                if (it.second == null) {
                    ItemRange(it.first)
                } else {
                    val item = it.first
                    val range = it.second!!.toMilliseconds()
                    ItemRange(item, range.first, range.second)
                } },
            parameters = description.taskType.targetType.parameters

        )
    }

    abstract fun addSubmission(submission: Submission)
}