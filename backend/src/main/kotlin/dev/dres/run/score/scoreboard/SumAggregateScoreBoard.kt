package dev.dres.run.score.scoreboard

import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.run.Task
import dev.dres.data.model.run.TaskId
import dev.dres.run.score.interfaces.TaskRunScorer

class SumAggregateScoreBoard(override val name: String, private val boards: List<Scoreboard>, private val taskGroupName: String? = null) : Scoreboard {

    override fun scores(): List<Score> = this.boards.map { it.scores() }
        .flatten().groupBy { it.teamId }.values
        .map { Score(it.first().teamId, it.map { it.score }.sum()) }

    override fun score(teamId: TeamId) = boards.map { it.score(teamId) }.sum()


    override fun update(runs: List<Task>) {
        //since calls are delegated, nothing needs to be done here
    }

    override fun update(scorers: Map<TaskId, TaskRunScorer>) {
        //since calls are delegated, nothing needs to be done here
    }


    override fun overview() = ScoreOverview(this.name, this.taskGroupName, scores())
}