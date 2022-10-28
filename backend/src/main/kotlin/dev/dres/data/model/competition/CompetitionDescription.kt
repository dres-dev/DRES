package dev.dres.data.model.competition

import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.admin.User
import dev.dres.data.model.competition.task.TaskDescription
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.MediaType
import dev.dres.data.model.competition.task.TaskGroup
import dev.dres.data.model.competition.task.TaskType
import dev.dres.data.model.competition.team.Team
import dev.dres.data.model.competition.team.TeamGroup
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
    var name by xdRequiredStringProp(unique = true, trimmed = false)

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
    val judges by xdLink0_N(User::judges, onDelete = OnDeletePolicy.CLEAR, onTargetDelete = OnDeletePolicy.CLEAR)

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
            MaxNormalizingScoreBoard(group.name, teams, {task -> task.taskGroup.id == group.id}, group.name)
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
    fun getAllVideos(): List<MediaItem> {
        return (this.taskGroups.flatMapDistinct { it.tasks }
            .flatMapDistinct { it.hints }
            .filter { it.hintItem ne null }
            .mapDistinct { it.hintItem } union
        this.taskGroups.flatMapDistinct { it.tasks }
            .flatMapDistinct { it.targets }
            .filter { it.item ne null }
            .mapDistinct { it.item }).filter {
                it.type eq MediaType.VIDEO
            }.toList()
    }
}