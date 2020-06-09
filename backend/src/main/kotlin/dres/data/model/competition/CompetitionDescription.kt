package dres.data.model.competition

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import dres.data.model.Entity
import dres.run.score.scoreboard.MaxNormalizingScoreBoard
import dres.run.score.scoreboard.MeanAggregateScoreBoard
import dres.run.score.scoreboard.Scoreboard
import java.util.*


data class CompetitionDescription @JsonCreator constructor(
        @JsonProperty("id") override var id: Long,
        @JsonProperty("name") val name: String,
        @JsonProperty("description") val description: String?,
        @JsonProperty("groups") val groups: MutableList<TaskGroup>,
        @JsonProperty("tasks") val tasks: MutableList<TaskDescriptionBase>,
        @JsonProperty("teams") val teams: MutableList<Team>,
        @JsonProperty("uid") val uid: String = UUID.randomUUID().toString()
) : Entity {

    fun validate() {
        for (group in this.groups) {
            if (this.groups.map { it.name }.count { it == group.name } > 1) {
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
    }

    /**
     * Generates and returns the default [Scoreboard] implementations for this [CompetitionDescription]
     *
     * @return List of [Scoreboard] implementations.
     */
    fun generateDefaultScoreboards(): List<Scoreboard> {
        val groupBoards = this.groups.map {group ->
            MaxNormalizingScoreBoard(group.name, {task -> task.taskGroup == group}, group.name)
        }
        val aggregateScoreBoard = MeanAggregateScoreBoard("average", groupBoards)
        return groupBoards.plus(aggregateScoreBoard)
    }
}