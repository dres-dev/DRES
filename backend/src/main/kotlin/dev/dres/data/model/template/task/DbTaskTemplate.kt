package dev.dres.data.model.template.task

import dev.dres.api.rest.types.competition.tasks.*
import dev.dres.api.rest.types.competition.team.ApiTeam
import dev.dres.api.rest.types.task.ApiContentElement
import dev.dres.api.rest.types.task.ApiContentType
import dev.dres.data.model.Config
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.run.interfaces.TaskRun
import dev.dres.data.model.template.TemplateId
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.*
import kotlinx.dnq.simple.min
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.IllegalStateException
import java.lang.Long.max
import java.util.*

/**
 * Basic description of a [TaskRun] as executed in DRES. Defines basic attributes such as its name, its duration,
 * the [DbTaskTemplateTarget] and the [DbHint]s, that should be presented to the user.
 *
 * @version 2.0.0
 * @author Luca Rossetto
 * @author Ralph Gasser
 */
class DbTaskTemplate(entity: Entity) : PersistentEntity(entity), TaskTemplate {
    companion object: XdNaturalEntityType<DbTaskTemplate>()

    /** The [TemplateId] of this [DbTaskTemplate]. */
    override var templateId: TemplateId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [DbTeam]. Must be unique!*/
    var name by xdRequiredStringProp(unique = false, trimmed = true)

    /** The [DbTaskGroup] this [DbTaskTemplate] belongs to. */
    var taskGroup by xdLink1(DbTaskGroup)

    /** The [DbEvaluationTemplate] this [DbTaskTemplate] belongs to. */
    var evaluation: DbEvaluationTemplate by xdParent<DbTaskTemplate,DbEvaluationTemplate>(DbEvaluationTemplate::tasks)

    /** The [DbMediaCollection] this [DbTaskTemplate] operates upon. */
    var collection by xdLink1(DbMediaCollection)

    /** The duration of the [DbTaskTemplate] in seconds. */
    var duration by xdRequiredLongProp { min(0L)  }

    /** The [DbTaskTemplateTarget]s that identify the target. Multiple entries indicate the existence of multiple targets. */
    val targets by xdChildren1_N<DbTaskTemplate, DbTaskTemplateTarget>(DbTaskTemplateTarget::task)

    /** The [DbHint]s that act as clues to find the target media. */
    val hints by xdChildren0_N<DbTaskTemplate,DbHint>(DbHint::task)

    /**
     * Creates a [DbTaskTemplate] instance and returns it.
     *
     * @return [DbTaskTemplate] instance
     */
    fun toInstance(forEvaluation: DbEvaluationTemplate): DbTaskTemplate {
        require(!this.evaluation.instance) { throw IllegalStateException("Cannot create an instance of an TaskTemplate that is an instance itself.") }
        require(forEvaluation.instance) { throw IllegalStateException("Cannot attach an instance of an TaskTemplate to an EvaluationTemplate that is not an instance itself.") }
        val copy = DbTaskTemplate.new {
            this.id = UUID.randomUUID().toString()
            this.name = this@DbTaskTemplate.name
            this.collection = this@DbTaskTemplate.collection
            this.duration = this@DbTaskTemplate.duration
            this.taskGroup = forEvaluation.taskGroups.query(DbTaskGroup::name eq this@DbTaskTemplate.taskGroup.name).first()
        }

        /* Copy task targets. */
        this.targets.asSequence().forEach {
            copy.targets.add(
                DbTaskTemplateTarget.new {
                    this.type = it.type
                    this.start = it.start
                    this.end = it.end
                    this.item = it.item
                    this.text = it.text
                }
            )
        }

        /* Copy task hints. */
        this.hints.asSequence().forEach {
            copy.hints.add(
                DbHint.new {
                    this.type = it.type
                    this.start = it.start
                    this.end = it.end
                    this.item = it.item
                    this.text = it.text
                    this.path = it.path
                    this.temporalRangeStart = it.temporalRangeStart
                    this.temporalRangeEnd = it.temporalRangeEnd
                }
            )
        }

        return copy
    }

    /**
     * Generates and returns a [ApiHintContent] object to be used by the RESTful interface.
     *
     * Requires a valid transaction.
     *
     * @return [ApiHintContent]
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    fun toTaskHint(): ApiHintContent {
        val sequence = this.hints.asSequence().groupBy { it.type }.flatMap { group ->
            var index = 0
            group.value.sortedBy { it.start ?: 0 }.flatMap {
                val ret = mutableListOf(it.toQueryContentElement())
                if (it.end != null) {
                    if (index == (group.value.size - 1)) {
                        ret.add(ApiContentElement(contentType = ret.first().contentType, offset = it.end!!))
                    } else if ((group.value[index+1].start ?: 0) > it.end!!) {
                        ret.add(ApiContentElement(contentType = ret.first().contentType, offset = it.end!!))
                    }
                }
                index += 1
                ret
            }
        }
        return ApiHintContent(this.id, sequence, false)
    }

    /**
     * Generates and returns a [ApiTargetContent] object to be used by the RESTful interface.
     *
     * Requires a valid transaction.
     *
     * @return [ApiTargetContent]
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    fun toTaskTarget(): ApiTargetContent {
        var cummulativeOffset = 0L
        val sequence = this.targets.asSequence().flatMap {
            cummulativeOffset += Math.floorDiv(it.item?.durationMs ?: 10000L, 1000L) + 1L
            listOf(
                it.toQueryContentElement(),
                ApiContentElement(ApiContentType.EMPTY, null, cummulativeOffset)
            )
        }.toList()
        return ApiTargetContent(this.id, sequence)
    }

    /**
     * Produces a Textual description of the content of the [DbTaskTemplate] if possible
     *
     * @return Textual description of this [DbTaskTemplate]'s content,
     */
    override fun textualDescription(): String = this.hints.asSequence().filter { it.type == DbHintType.TEXT }.maxByOrNull { it.start ?: 0 }?.text ?: name

    /**
     * Converts this [DbTaskTemplate] to a RESTful API representation [ApiTaskTemplate].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [ApiTeam]
     */
    fun toApi(): ApiTaskTemplate = ApiTaskTemplate(
        this.id,
        this.name,
        this.taskGroup.name,
        this.taskGroup.type.name,
        this.duration,
        this.collection.id,
        this.targets.asSequence().map { it.toApi() }.toList(),
        this.hints.asSequence().map { it.toApi() }.toList()
    )

    /**
     * Checks if no components of the same type overlap
     *
     * @throws IllegalArgumentException
     */

    fun validate() {
        this.hints.asSequence().groupBy { it.type }.forEach { group ->
            var end = 0L
            group.value.sortedBy { it.start ?: 0 }.forEach {
                if((it.start ?: end) < end){
                    throw IllegalArgumentException("Overlapping component of type ${group.key} in task $name")
                }
                end = max(end, it.end ?: ((it.start ?: 0L) + 1L))
            }
        }
    }
}