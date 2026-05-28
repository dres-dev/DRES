package dev.dres.data.model.run

import dev.dres.data.model.template.task.options.DbTargetOption
import dev.dres.run.validation.*
import dev.dres.run.validation.interfaces.AnswerSetValidator
import dev.dres.run.validation.judged.BasicJudgementValidator
import dev.dres.run.validation.judged.ItemRange
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*

/**
 * An abstract [DbTask] implementation for non-interactive [DbTask], i.e., [DbTask]s that do not rely on human interaction and simply process input data in batches
 *
 * @author Luca Rossetto
 * @author Ralph Gasser
 * @version 2.0.0
 */
abstract class AbstractNonInteractiveTask(store: TransientEntityStore, task: DbTask) : AbstractTask(store, task) {
    /** The [AnswerSetValidator] used by this [AbstractNonInteractiveTask]. */
    final override val validator: AnswerSetValidator

    init {
        this.validator = store.transactional(true) {
            val template = task.template
            when (val targetOption = template.taskGroup.type.target) {
                DbTargetOption.MEDIA_ITEM -> MediaItemsAnswerSetValidator(template.targets.mapDistinct { it.item }
                    .filter { it ne null }.toSet())

                DbTargetOption.MEDIA_SEGMENT -> {
                    val target =
                        template.targets.filter { (it.item ne null) and (it.start ne null) and (it.end ne null) }
                            .asSequence().map { TransientMediaSegment(it.item!!, it.range!!) }.toSet()
                    TemporalContainmentAnswerSetValidator(target)
                }

                DbTargetOption.TEXT -> TextAnswerSetValidator(
                    template.targets.filter { it.text ne null }.asSequence().map { it.text!! }.toList()
                )

                DbTargetOption.JUDGEMENT -> {
                    val knownRanges =
                        template.targets.filter { (it.item ne null) and (it.start ne null) and (it.end ne null) }
                            .asSequence().map {
                            ItemRange(it.item?.name!!, it.start!!, it.end!!)
                        }.toSet()
                    BasicJudgementValidator(template.toApi(), this.store, template.taskGroup.type.toApi(), knownCorrectRanges = knownRanges)
                }

                else -> throw IllegalStateException("The provided target option ${targetOption.description} is not supported by interactive tasks.")
            }
        }
    }
}
