package dev.dres.data.model.run

import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.template.task.options.DbTargetOption
import dev.dres.data.model.template.team.TeamAggregatorImpl
import dev.dres.data.model.template.team.TeamGroupId
import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.submissions.DbSubmission
import dev.dres.run.validation.MediaItemsAnswerSetValidator
import dev.dres.run.validation.TemporalOverlapAnswerSetValidator
import dev.dres.run.validation.TextAnswerSetValidator
import dev.dres.run.validation.TransientMediaSegment
import dev.dres.run.validation.interfaces.AnswerSetValidator
import dev.dres.run.validation.judged.BasicJudgementValidator
import dev.dres.run.validation.judged.BasicVoteValidator
import dev.dres.run.validation.judged.ItemRange
import kotlinx.dnq.query.*

/**
 * An abstract [DbTask] implementation for interactive [DbTask], i.e. [DbTask]s that rely on human interaction, such as [DbSubmission]s
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.0
 */
abstract class AbstractInteractiveTask(task: DbTask): AbstractTask(task) {


    /** The total duration in milliseconds of this task. Usually determined by the [DbTaskTemplate] but can be adjusted! */
    override abstract var duration: Long

    /** The [AnswerSetValidator] used to validate [DbSubmission]s. */
    final override val validator: AnswerSetValidator

    init {
        this.validator = when (val targetOption = this.template.taskGroup.type.target) {
            DbTargetOption.MEDIA_ITEM -> MediaItemsAnswerSetValidator(this.template.targets.filter { it.item ne null }.mapDistinct { it.item }.toSet())
            DbTargetOption.MEDIA_SEGMENT -> {
                val target = this.template.targets.filter { (it.item ne null) and (it.start ne null) and (it.end ne null)}.asSequence().map { TransientMediaSegment(it.item!!, it.range!!) }.toSet()
                TemporalOverlapAnswerSetValidator(target)
            }
            DbTargetOption.TEXT -> TextAnswerSetValidator(this.template.targets.filter { it.text ne null }.asSequence().map { it.text!! }.toList())
            DbTargetOption.JUDGEMENT -> {
                val knownRanges = this.template.targets.filter { (it.item ne null) and (it.start ne null) and (it.end ne null) }.asSequence().map {
                    ItemRange(it.item?.name!!, it.start!!, it.end!!)
                }.toSet()
                BasicJudgementValidator(knownCorrectRanges = knownRanges)
            }
            DbTargetOption.VOTE -> {
                val knownRanges = this.template.targets.filter { (it.item ne null) and (it.start ne null) and (it.end ne null) }.asSequence().map {
                    ItemRange(it.item?.name!!, it.start!!, it.end!!)
                }.toSet()
                val parameters = this.template.taskGroup.type.configurations.filter { it.key eq targetOption.description }.asSequence().associate { it.key to it.value }
                BasicVoteValidator(knownCorrectRanges = knownRanges, parameters = parameters)
            }
            else -> throw IllegalStateException("The provided target option ${targetOption.description} is not supported by interactive tasks.")
        }
    }

}