package dev.dres.data.model.competition

import dev.dres.api.rest.types.competition.ApiCompetitionDescription
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.User
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.MediaType
import dev.dres.data.model.competition.task.TaskGroup
import dev.dres.data.model.competition.task.TaskType
import dev.dres.data.model.competition.team.Team
import dev.dres.data.model.competition.team.TeamGroup
import dev.dres.data.model.media.time.TemporalRange
import dev.dres.run.score.scoreboard.MaxNormalizingScoreBoard
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.score.scoreboard.SumAggregateScoreBoard
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy
import kotlinx.dnq.query.*

typealias CompetitionDescriptionId = String

/**
 * Basic description of a competitions as executed in DRES.
 *
 * Defines basic attributes such as its name and the [TaskType]s and [TaskGroup]s it contains.
 *
 * @version 2.0.0
 * @author Luca Rossetto & Ralph Gasser
 */
class CompetitionDescription(entity: Entity) : PersistentEntity(entity){
    companion object: XdNaturalEntityType<CompetitionDescription>()

    /** The [CompetitionDescriptionId] of this [CompetitionDescription]. */
    var teamId: CompetitionDescriptionId
        get() = this.id
        set(value) { this.id = value }

    /** The name held by this [CompetitionDescription]. Must be unique!*/
    var name by xdRequiredStringProp(unique = true, trimmed = true)

    /** If set, this [CompetitionDescription] is considered a template!*/
    var template by xdBooleanProp()

    /** An optional description of this [CompetitionDescription]. */
    var description by xdStringProp(trimmed = false)

    /** The [TaskType]s defined within this [CompetitionDescription]. */
    val taskTypes by xdChildren0_N<CompetitionDescription, TaskType>(TaskType::competition)

    /** The [TaskGroup]s that are part of this [CompetitionDescription]. */
    val taskGroups by xdChildren0_N<CompetitionDescription, TaskGroup>(TaskGroup::competition)

    /** The [Team]s that are part of this [CompetitionDescription]. */
    val teams by xdChildren0_N<CompetitionDescription,Team>(Team::competition)

    /** The [Team]s that are part of this [CompetitionDescription]. */
    val teamsGroups by xdChildren0_N<CompetitionDescription,TeamGroup>(TeamGroup::competition)

    /** The [User]s that act as judge for this [CompetitionDescription] */
    val judges by xdLink0_N(User, onDelete = OnDeletePolicy.CLEAR, onTargetDelete = OnDeletePolicy.CLEAR)

    /**
     * Converts this [CompetitionDescription] to a RESTful API representation [ApiCompetitionDescription].
     *
     * This is a convenience method and it requires and active transaction context.
     *
     * @return [ApiCompetitionDescription]
     */
    fun toApi(): ApiCompetitionDescription = ApiCompetitionDescription(
        id = this.id,
        name = this.name,
        description = this.description,
        taskTypes = this.taskTypes.asSequence().map { it.toApi() }.toList(),
        taskGroups = this.taskGroups.asSequence().map { it.toApi() }.toList(),
        teams = this.teams.asSequence().map { it.toApi() }.toList(),
        teamGroups = this.teamsGroups.asSequence().map { it.toApi() }.toList(),
        judges = this.judges.asSequence().map { it.id }.toList()
    )

    /**
     * Generates and returns the default [Scoreboard] implementations for this [CompetitionDescription].
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
     * Generates and returns a list of all [MediaItem] for this [CompetitionDescription].
     *
     * This is a convenience method and requires an active transaction context.
     *
     * @return [List] of [MediaItem]s
     */
    fun getAllVideos(): List<Pair<MediaItem,TemporalRange>> {
        val hints = this.taskGroups.flatMapDistinct { it.tasks }
            .flatMapDistinct { it.hints }
            .filter { (it.item ne null) and (it.item!!.type eq MediaType.VIDEO) }.asSequence().map {
                it.item!!to it.range!!
            }

        val targets = this.taskGroups.flatMapDistinct { it.tasks }
            .flatMapDistinct { it.targets }
            .filter { (it.item ne null) and (it.item!!.type eq MediaType.VIDEO) }.asSequence().map {
            it.item!! to it.range!!
        }

        return (hints + targets).toList()
    }
}