package dev.dres.data.model.template

import dev.dres.api.rest.types.competition.ApiEvaluationTemplate
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.User
import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.MediaType
import dev.dres.data.model.template.task.TaskGroup
import dev.dres.data.model.template.task.TaskType
import dev.dres.data.model.template.team.Team
import dev.dres.data.model.template.team.TeamGroup
import dev.dres.data.model.media.time.TemporalRange
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
 * Defines basic attributes such as its name and the [TaskType]s and [TaskGroup]s it contains.
 *
 * @version 2.0.0
 * @author Luca Rossetto & Ralph Gasser
 */
class EvaluationTemplate(entity: Entity) : PersistentEntity(entity){
    companion object: XdNaturalEntityType<EvaluationTemplate>()

    /** The [TemplateId] of this [EvaluationTemplate]. */
    var teamId: TemplateId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [EvaluationTemplate]. Must be unique!*/
    var name by xdRequiredStringProp(unique = true, trimmed = true)

    /** If set, this [EvaluationTemplate] is considered a template!*/
    var template by xdBooleanProp()

    /** An optional description of this [EvaluationTemplate]. */
    var description by xdStringProp(trimmed = false)

    /** The [TaskType]s defined within this [EvaluationTemplate]. */
    val taskTypes by xdChildren0_N<EvaluationTemplate, TaskType>(TaskType::competition)

    /** The [TaskGroup]s that are part of this [EvaluationTemplate]. */
    val taskGroups by xdChildren0_N<EvaluationTemplate, TaskGroup>(TaskGroup::competition)

    /** The [TaskTemplate]s contained in this [EvaluationTemplate]*/
    val tasks by xdChildren0_N<EvaluationTemplate, TaskTemplate>(TaskTemplate::competition)

    /** The [Team]s that are part of this [EvaluationTemplate]. */
    val teams by xdChildren0_N<EvaluationTemplate,Team>(Team::template)

    /** The [Team]s that are part of this [EvaluationTemplate]. */
    val teamsGroups by xdChildren0_N<EvaluationTemplate,TeamGroup>(TeamGroup::template)

    /** The [User]s that act as judge for this [EvaluationTemplate] */
    val judges by xdLink0_N(User, onDelete = OnDeletePolicy.CLEAR, onTargetDelete = OnDeletePolicy.CLEAR)

    /**
     * Converts this [EvaluationTemplate] to a RESTful API representation [ApiEvaluationTemplate].
     *
     * This is a convenience method and it requires and active transaction context.
     *
     * @return [ApiEvaluationTemplate]
     */
    fun toApi(): ApiEvaluationTemplate = ApiEvaluationTemplate(
        id = this.id,
        name = this.name,
        description = this.description,
        taskTypes = this.taskTypes.asSequence().map { it.toApi() }.toList(),
        taskGroups = this.taskGroups.asSequence().map { it.toApi() }.toList(),
        tasks = this.tasks.asSequence().map { it.toApi() }.toList(),
        teams = this.teams.asSequence().map { it.toApi() }.toList(),
        teamGroups = this.teamsGroups.asSequence().map { it.toApi() }.toList(),
        judges = this.judges.asSequence().map { it.id }.toList()
    )

    /**
     * Generates and returns the default [Scoreboard] implementations for this [EvaluationTemplate].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return List of [Scoreboard] implementations.
     */
    fun generateDefaultScoreboards(): List<Scoreboard> {
        val teams = this.teams.toList()
        val groupBoards = this.taskGroups.asSequence().map { group ->
            MaxNormalizingScoreBoard(group.name, teams, {task -> task.taskGroup.name == group.name}, group.name)
        }.toList()
        val aggregateScoreBoard = SumAggregateScoreBoard("sum", groupBoards)
        return groupBoards.plus(aggregateScoreBoard)
    }

    /**
     * Generates and returns a list of all [MediaItem] for this [EvaluationTemplate].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [List] of [MediaItem]s
     */
    fun getAllVideos(): List<Pair<MediaItem,TemporalRange>> {
        val hints = this.tasks
            .flatMapDistinct { it.hints }
            .filter { (it.item ne null) and (it.item!!.type eq MediaType.VIDEO) }.asSequence().map {
                it.item!!to it.range!!
            }

        val targets = this.tasks
            .flatMapDistinct { it.targets }
            .filter { (it.item ne null) and (it.item!!.type eq MediaType.VIDEO) }.asSequence().map {
            it.item!! to it.range!!
        }

        return (hints + targets).toList()
    }
}