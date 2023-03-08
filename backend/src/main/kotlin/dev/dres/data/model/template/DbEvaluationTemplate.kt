package dev.dres.data.model.template

import dev.dres.api.rest.types.competition.ApiEvaluationTemplate
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
import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.template.interfaces.EvaluationTemplate
import dev.dres.run.score.scoreboard.MaxNormalizingScoreBoard
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.score.scoreboard.SumAggregateScoreBoard
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.query.*

typealias TemplateId = String

/**
 * Basic description of a competitions as executed in DRES.
 *
 * Defines basic attributes such as its name and the [DbTaskType]s and [DbTaskGroup]s it contains.
 *
 * @version 2.0.0
 * @author Luca Rossetto & Ralph Gasser
 */
class DbEvaluationTemplate(entity: Entity) : PersistentEntity(entity), EvaluationTemplate{
    companion object: XdNaturalEntityType<DbEvaluationTemplate>()

    /** The [TemplateId] of this [DbEvaluationTemplate]. */
    override var templateId: TemplateId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [DbEvaluationTemplate]. Must be unique!*/
    var name by xdRequiredStringProp(trimmed = true)

    /** An optional description of this [DbEvaluationTemplate]. */
    var description by xdStringProp(trimmed = false)

    /** The [DbTaskType]s defined within this [DbEvaluationTemplate]. */
    val taskTypes by xdChildren0_N<DbEvaluationTemplate, DbTaskType>(DbTaskType::evaluation)

    /** The [DbTaskGroup]s that are part of this [DbEvaluationTemplate]. */
    val taskGroups by xdChildren0_N<DbEvaluationTemplate, DbTaskGroup>(DbTaskGroup::evaluation)

    /** The [DbTaskTemplate]s contained in this [DbEvaluationTemplate]*/
    val tasks by xdChildren0_N<DbEvaluationTemplate, DbTaskTemplate>(DbTaskTemplate::evaluation)

    /** The [DbTeam]s that are part of this [DbEvaluationTemplate]. */
    val teams by xdChildren0_N<DbEvaluationTemplate,DbTeam>(DbTeam::evaluation)

    /** The [DbTeam]s that are part of this [DbEvaluationTemplate]. */
    val teamGroups by xdChildren0_N<DbEvaluationTemplate,DbTeamGroup>(DbTeamGroup::evaluation)

    /** The [DbUser]s that act as judge for this [DbEvaluationTemplate] */
    val judges by xdLink0_N(DbUser, onDelete = OnDeletePolicy.CLEAR, onTargetDelete = OnDeletePolicy.CLEAR)

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
        canBeEdited = this.canBeEdited(),
        taskTypes = this.taskTypes.asSequence().map { it.toApi() }.toList(),
        taskGroups = this.taskGroups.asSequence().map { it.toApi() }.toList(),
        tasks = this.tasks.asSequence().map { it.toApi() }.toList(),
        teams = this.teams.asSequence().map { it.toApi() }.toList(),
        teamGroups = this.teamGroups.asSequence().map { it.toApi() }.toList(),
        judges = this.judges.asSequence().map { it.id }.toList()
    )

    /**
     * Checks if this [DbEvaluationTemplate] can be edited. This is the case only, if no active [DbEvaluation] exists that uses it.
     *
     * This is a convenience method and requires an active transaction context.
     */
    fun canBeEdited() = DbEvaluation.filter { it.template.id eq this@DbEvaluationTemplate.id }.none()

    /**
     * Generates and returns a list of all [DbMediaItem] for this [DbEvaluationTemplate].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [List] of [DbMediaItem]s
     */
    fun getAllVideos(): List<Pair<DbMediaItem,TemporalRange>> {
        val hints = this.tasks
            .flatMapDistinct { it.hints }
            .filter { (it.item ne null) and (it.item!!.type eq DbMediaType.VIDEO) }.asSequence().map {
                it.item!!to it.range!!
            }

        val targets = this.tasks
            .flatMapDistinct { it.targets }
            .filter { (it.item ne null) and (it.item!!.type eq DbMediaType.VIDEO) }.asSequence().map {
            it.item!! to it.range!!
        }

        return (hints + targets).toList()
    }
}