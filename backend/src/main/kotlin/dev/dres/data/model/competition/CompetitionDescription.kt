package dev.dres.data.model.competition


import dev.dres.data.model.Entity
import dev.dres.data.model.UID
import dev.dres.run.score.scoreboard.MaxNormalizingScoreBoard
import dev.dres.run.score.scoreboard.MeanAggregateScoreBoard
import dev.dres.run.score.scoreboard.Scoreboard


data class CompetitionDescription(
        override var id: UID,
        val name: String,
        val description: String?,
        val taskTypes: MutableList<TaskType>,
        val taskGroups: MutableList<TaskGroup>,
        val tasks: MutableList<TaskDescription>,
        val teams: MutableList<Team>,
        val participantCanView: Boolean
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
        val aggregateScoreBoard = MeanAggregateScoreBoard("average", groupBoards)
        return groupBoards.plus(aggregateScoreBoard)
    }

    fun getAllCachedVideoItems(): List<CachedVideoItem> = tasks.flatMap { it.hints }.filterIsInstance(CachedVideoItem::class.java).plus(
            tasks.map { it.target }.filterIsInstance(CachedVideoItem::class.java)
    )
}