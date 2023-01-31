package dev.dres.data.model.run

import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.template.task.options.TargetOption
import dev.dres.data.model.template.team.TeamAggregatorImpl
import dev.dres.data.model.template.team.TeamGroupId
import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.submissions.Submission
import dev.dres.run.validation.MediaItemsSubmissionValidator
import dev.dres.run.validation.TemporalOverlapSubmissionValidator
import dev.dres.run.validation.TextValidator
import dev.dres.run.validation.TransientMediaSegment
import dev.dres.run.validation.interfaces.SubmissionValidator
import dev.dres.run.validation.judged.BasicJudgementValidator
import dev.dres.run.validation.judged.BasicVoteValidator
import dev.dres.run.validation.judged.ItemRange
import kotlinx.dnq.query.*

/**
 * An abstract [Task] implementation for interactive [Task], i.e. [Task]s that rely on human interaction, such as [Submission]s
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractInteractiveTask(task: Task): AbstractTask(task) {


    /** The total duration in milliseconds of this task. Usually determined by the [TaskTemplate] but can be adjusted! */
    abstract var duration: Long

    /** Map of [TeamGroupId] to [TeamAggregatorImpl]. */
    val teamGroupAggregators: Map<TeamGroupId, TeamAggregatorImpl> by lazy {
        this.competition.description.teamsGroups.asSequence().associate { it.id to it.newAggregator() }
    }

    /** The [SubmissionValidator] used to validate [Submission]s. */
    final override val validator: SubmissionValidator

    init {
        this.validator = when (val targetOption = this.template.taskGroup.type.target) {
            TargetOption.MEDIA_ITEM -> MediaItemsSubmissionValidator(this.template.targets.mapDistinct { it.item }.filter { it ne null }.toSet())
            TargetOption.MEDIA_SEGMENT -> {
                val target = this.template.targets.filter { (it.item ne null) and (it.start ne null) and (it.end ne null)}.take(1) .first()
                TemporalOverlapSubmissionValidator(TransientMediaSegment(target.item!!, target.range!!))
            }
            TargetOption.TEXT -> TextValidator(this.template.targets.filter { it.text ne null }.asSequence().map { it.text!! }.toList())
            TargetOption.JUDGEMENT -> {
                val knownRanges = this.template.targets.filter { (it.item ne null) and (it.start ne null) and (it.end ne null) }.asSequence().map {
                    ItemRange(it.item?.name!!, it.start!!, it.end!!)
                }.toSet()
                BasicJudgementValidator(knownCorrectRanges = knownRanges)
            }
            TargetOption.VOTE -> {
                val knownRanges = this.template.targets.filter { (it.item ne null) and (it.start ne null) and (it.end ne null) }.asSequence().map {
                    ItemRange(it.item?.name!!, it.start!!, it.end!!)
                }.toSet()
                val parameters = this.template.taskGroup.type.configurations.filter { it.key eq targetOption.description }.asSequence().associate { it.key to it.value }
                BasicVoteValidator(knownCorrectRanges = knownRanges, parameters = parameters)
            }
            else -> throw IllegalStateException("The provided target option ${targetOption.description} is not supported by interactive tasks.")
        }
    }

    /**
     * Updates the per-team aggregation for this [AbstractInteractiveTask].
     *
     * @param teamScores Map of team scores.
     */
    internal fun updateTeamAggregation(teamScores: Map<TeamId, Double>) {
        this.teamGroupAggregators.values.forEach { it.aggregate(teamScores) }
    }
}