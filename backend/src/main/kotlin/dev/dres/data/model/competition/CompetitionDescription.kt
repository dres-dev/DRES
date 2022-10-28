package dev.dres.data.model.competition

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.data.model.Config
import dev.dres.data.model.PersistentEntity
import dev.dres.data.model.UID
import dev.dres.data.model.admin.User
import dev.dres.data.model.admin.UserId
import dev.dres.data.model.competition.team.Team
import dev.dres.data.model.competition.team.TeamGroup
import dev.dres.data.model.competition.team.TeamGroupId
import dev.dres.run.score.scoreboard.MaxNormalizingScoreBoard
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.score.scoreboard.SumAggregateScoreBoard
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.link.OnDeletePolicy
import java.nio.file.Paths

/**
 *
 */
class CompetitionDescription(entity: Entity) : PersistentEntity(entity){
    companion object: XdNaturalEntityType<CompetitionDescription>()

    /** The name held by this [CompetitionDescription]. Must be unique!*/
    var name by xdRequiredStringProp(unique = true, trimmed = false)

    /** An optional description of this [CompetitionDescription]. */
    var description by xdStringProp(trimmed = false)

    /** The [TaskGroup]s that are part of this [CompetitionDescription]. */
    val taskGroups by xdChildren0_N<CompetitionDescription,TaskGroup>(TaskGroup::competition)

    /** The [Team]s that are part of this [CompetitionDescription]. */
    val teams by xdChildren0_N<CompetitionDescription,Team>(Team::competition)

    /** The [Team]s that are part of this [CompetitionDescription]. */
    val teamsGroups by xdChildren0_N<CompetitionDescription,TeamGroup>(TeamGroup::competition)

    /** The [User]s that act as judge for this [CompetitionDescription] */
    val judges by xdLink0_N(User::judges, onDelete = OnDeletePolicy.CLEAR, onTargetDelete = OnDeletePolicy.CLEAR)

    /*
    val taskTypes: MutableList<TaskType>,
    val tasks: MutableList<TaskDescription>,
  */

    fun validate() {
        for (group in this.taskGroups) {
            if (this.taskGroups.map { it.name }.count { it == group.name } > 1) {
                throw IllegalArgumentException("Duplicate group with name '${group.name}'!")
            }
        }

        for (task in this.tasks) {
            if (this.tasks.map { it.name }.count { it == task.name } > 1) {
                throw IllegalArgumentException("Duplicate task with name '${task.name}'!")
            }
        }

        for (team in this.teams) {
            if (this.teams.map { it.name }.count { it == team.name } > 1) {
                throw IllegalArgumentException("Duplicate team with name '${team.name}'!")
            }
        }

        tasks.forEach { it.validate() }
    }

    /**
     * Generates and returns the default [Scoreboard] implementations for this [CompetitionDescription]
     *
     * @return List of [Scoreboard] implementations.
     */
    fun generateDefaultScoreboards(): List<Scoreboard> {
        val groupBoards = this.taskGroups.map { group ->
            MaxNormalizingScoreBoard(group.name, this.teams, {task -> task.taskGroup == group}, group.name)
        }
        val aggregateScoreBoard = SumAggregateScoreBoard("sum", groupBoards)
        return groupBoards.plus(aggregateScoreBoard)
    }

    /**
     * Generates and returns a list of all [CachedVideoItem] for this [CompetitionDescription].
     *
     * This is a convenience method and cannot be serialized!
     *
     * @return [List] of [CachedVideoItem]s
     */
    @JsonIgnore
    fun getAllCachedVideoItems(): List<CachedVideoItem> = this.tasks
        .flatMap { it.hints }
        .filterIsInstance(CachedVideoItem::class.java)
        .plus(tasks.map { it.target }.filterIsInstance(CachedVideoItem::class.java))
}