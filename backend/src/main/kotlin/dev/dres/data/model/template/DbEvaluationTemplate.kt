package dev.dres.data.model.template

import dev.dres.api.rest.types.template.ApiEvaluationTemplate
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.DbMediaType
import dev.dres.data.model.template.task.DbTaskGroup
import dev.dres.data.model.template.task.DbTaskType
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.template.team.DbTeamGroup
import dev.dres.data.model.media.time.TemporalRange
import dev.dres.data.model.template.interfaces.EvaluationTemplate
import dev.dres.data.model.template.task.DbHintType
import dev.dres.data.model.template.task.options.DbConfiguredOption
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.query.*
import java.lang.IllegalStateException
import java.nio.file.Path
import java.util.*

typealias TemplateId = String

/**
 * Basic description of a competitions as executed in DRES.
 *
 * Defines basic attributes such as its name and the [DbTaskType]s and [DbTaskGroup]s it contains.
 *
 * @version 2.0.0
 * @author Luca Rossetto & Ralph Gasser
 */
class DbEvaluationTemplate(entity: Entity) : PersistentEntity(entity), EvaluationTemplate {
    companion object: XdNaturalEntityType<DbEvaluationTemplate>()

    /** The [TemplateId] of this [DbEvaluationTemplate]. */
    override var templateId: TemplateId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [DbEvaluationTemplate]. Must be unique!*/
    var name by xdRequiredStringProp(trimmed = true)

    /** An optional description of this [DbEvaluationTemplate]. */
    var description by xdStringProp(trimmed = false)

    /** Flag indicating, whether this [DbTaskTemplate] is a concrete instance or a general template. */
    var instance by xdBooleanProp()

    /** The [DbTaskType]s defined within this [DbEvaluationTemplate]. */
    val taskTypes by xdChildren0_N<DbEvaluationTemplate, DbTaskType>(DbTaskType::evaluation)

    /** The [DbTaskGroup]s that are part of this [DbEvaluationTemplate]. */
    val taskGroups by xdChildren0_N<DbEvaluationTemplate, DbTaskGroup>(DbTaskGroup::evaluation)

    /** The [DbTeam]s that are part of this [DbEvaluationTemplate]. */
    val teamGroups by xdChildren0_N<DbEvaluationTemplate,DbTeamGroup>(DbTeamGroup::evaluation)

    /** The [DbTeam]s that are part of this [DbEvaluationTemplate]. */
    val teams by xdChildren0_N<DbEvaluationTemplate,DbTeam>(DbTeam::evaluation)

    /** The [DbUser]s that act as judge for this [DbEvaluationTemplate] */
    val judges by xdLink0_N(DbUser, onDelete = OnDeletePolicy.CLEAR, onTargetDelete = OnDeletePolicy.CLEAR)

    /** The [DbTaskTemplate]s contained in this [DbEvaluationTemplate]*/
    val tasks by xdChildren0_N<DbEvaluationTemplate, DbTaskTemplate>(DbTaskTemplate::evaluation)

    /** A creation timestamp. */
    var created by xdDateTimeProp()

    /** A modification timestamp*/
    var modified by xdDateTimeProp()

    /**
     * Converts this [DbEvaluationTemplate] to a RESTful API representation [ApiEvaluationTemplate].
     *
     * This is a convenience method and it requires and active transaction context.
     *
     * @return [ApiEvaluationTemplate]
     */
    fun toApi(): ApiEvaluationTemplate = ApiEvaluationTemplate(
        id = this.id,
        name = this.name,
        description = this.description,
        created = this.created?.millis,
        modified = this.modified?.millis,
        taskTypes = this.taskTypes.asSequence().map { it.toApi() }.toList(),
        taskGroups = this.taskGroups.asSequence().map { it.toApi() }.toList(),
        teamGroups = this.teamGroups.asSequence().map { it.toApi() }.toList(),
        teams = this.teams.asSequence().map { it.toApi() }.toList(),
        judges = this.judges.asSequence().map { it.id }.toList(),
        tasks = this.tasks.asSequence().map { it.toApi() }.toList(),
    )

    /**
     * Creates a [DbTaskTemplate] instance and returns it.
     */
    fun toInstance(): DbEvaluationTemplate {
        require(!this.instance) { throw IllegalStateException("Cannot create an instance of an EvaluationTemplate that is an instance itself.") }
        val evaluation = DbEvaluationTemplate.new {
            id = UUID.randomUUID().toString()
            instance = true
            name = this@DbEvaluationTemplate.name
            description = this@DbEvaluationTemplate.description
            created = this@DbEvaluationTemplate.created
            modified = this@DbEvaluationTemplate.modified
            judges.addAll(this@DbEvaluationTemplate.judges)
        }

        /* Copy task types. */
        this.taskTypes.asSequence().forEach { t ->
            evaluation.taskTypes.add(
                DbTaskType.new {
                    name = t.name
                    duration = t.duration
                    target = t.target
                    score = t.score
                    hints.addAll(t.hints)
                    submission.addAll(t.submission)
                    options.addAll(t.options)
                    t.configurations.asSequence().forEach { c ->
                        configurations.add(DbConfiguredOption.new {
                            key = c.key
                            value = c.value
                        })
                    }
                }
            )
        }

        /* Copy task groups. */
        this.taskGroups.asSequence().forEach { g ->
            evaluation.taskGroups.add(
                DbTaskGroup.new {
                    name = g.name
                    type = evaluation.taskTypes.query((DbTaskType::name eq g.type.name)).first()
                }
            )
        }

        /* Copy team groups. */
        this.teamGroups.asSequence().forEach { g ->
            evaluation.teamGroups.add(
                DbTeamGroup.new {
                    id = UUID.randomUUID().toString()
                    name = g.name
                    defaultAggregator = g.defaultAggregator
                }
            )
        }

        /* Copy teams. */
        this.teams.asSequence().forEach { t ->
            evaluation.teams.add(
                DbTeam.new {
                    id = UUID.randomUUID().toString()
                    name = t.name
                    color = t.color
                    logo = t.logo
                    group = DbTeamGroup.query((DbTeamGroup::name eq t.group?.name)).firstOrNull()
                    users.addAll(t.users)
                }
            )
        }

        /* Copy tasks. */
        this.tasks.asSequence().forEach {
            evaluation.tasks.add(it.toInstance(evaluation))
        }
        return evaluation
    }

    /**
     * Generates and returns a collection of all [VideoSource]s for this [DbEvaluationTemplate].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [Set] of [VideoSource]s
     */
    fun getAllVideos(): Set<VideoSource> {
        val hints = this.tasks
            .flatMapDistinct { it.hints }
            .filter { it.type eq DbHintType.VIDEO }.asSequence().map {
                if (it.item != null) {
                    VideoSource.ItemSource(it.item!!, it.range!!)
                } else {
                    VideoSource.PathSource(it.path!!, it.range!!)
                }
            }

        val targets = this.tasks
            .flatMapDistinct { it.targets }
            .filter { (it.item ne null) and (it.item!!.type eq DbMediaType.VIDEO) }.asSequence()
            .map {
                VideoSource.ItemSource(it.item!!, it.range!!)
            }

        return (hints + targets).toSet()
    }

    sealed class VideoSource(val range: TemporalRange) {

        class ItemSource(val item: DbMediaItem, range: TemporalRange) : VideoSource(range)
        class PathSource(val path: String, range: TemporalRange) : VideoSource(range)


    }
}
