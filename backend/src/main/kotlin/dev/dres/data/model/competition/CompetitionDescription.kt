package dev.dres.data.model.competition

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.dres.data.model.Entity
import dev.dres.data.model.UID
import dev.dres.data.model.admin.UserId
import dev.dres.run.score.scoreboard.MaxNormalizingScoreBoard
import dev.dres.run.score.scoreboard.Scoreboard
import dev.dres.run.score.scoreboard.SumAggregateScoreBoard

data class CompetitionDescription(
    override var id: UID,
    val name: String,
    val description: String?,
    val taskTypes: MutableList<TaskType>,
    val taskGroups: MutableList<TaskGroup>,
    val tasks: MutableList<TaskDescription>,
    val teams: MutableList<Team>,
    val teamGroups: MutableList<TeamGroup>,
    val judges: MutableList<UserId>
) : Entity {

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