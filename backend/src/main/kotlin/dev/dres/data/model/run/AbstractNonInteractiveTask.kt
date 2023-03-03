package dev.dres.data.model.run

import dev.dres.data.model.template.task.options.DbTargetOption
import dev.dres.run.validation.MediaItemsAnswerSetValidator
import dev.dres.run.validation.TemporalOverlapAnswerSetValidator
import dev.dres.run.validation.TextAnswerSetValidator
import dev.dres.run.validation.TransientMediaSegment
import dev.dres.run.validation.interfaces.AnswerSetValidator
import dev.dres.run.validation.judged.BasicJudgementValidator
import dev.dres.run.validation.judged.ItemRange
import kotlinx.dnq.query.*

/**
 * An abstract [DbTask] implementation for non-interactive [DbTask], i.e., [DbTask]s that do not rely on human interaction and simply process input data in batches
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
abstract class AbstractNonInteractiveTask(task: DbTask): AbstractTask(task) {
    /** The [AnswerSetValidator] used by this [AbstractNonInteractiveTask]. */
    final override val validator: AnswerSetValidator

    init {
        this.validator = when (val targetOption = this.template.taskGroup.type.target) {
            DbTargetOption.MEDIA_ITEM -> MediaItemsAnswerSetValidator(this.template.targets.mapDistinct { it.item }.filter { it ne null }.toSet())
            DbTargetOption.MEDIA_SEGMENT -> {
                val target = this.template.targets.filter { (it.item ne null) and (it.start ne null) and (it.end ne null)}.take(1) .first()
                TemporalOverlapAnswerSetValidator(TransientMediaSegment(target.item!!, target.range!!))
            }
            DbTargetOption.TEXT -> TextAnswerSetValidator(this.template.targets.filter { it.text ne null }.asSequence().map { it.text!! }.toList())
            DbTargetOption.JUDGEMENT -> {
                val knownRanges = this.template.targets.filter { (it.item ne null) and (it.start ne null) and (it.end ne null) }.asSequence().map {
                    ItemRange(it.item?.name!!, it.start!!, it.end!!)
                }.toSet()
                BasicJudgementValidator(knownCorrectRanges = knownRanges)
            }
            else -> throw IllegalStateException("The provided target option ${targetOption.description} is not supported by interactive tasks.")
        }
    }
}