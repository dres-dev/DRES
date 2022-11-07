package dev.dres.run.score.scoreboard

import dev.dres.data.model.template.team.TeamId
import dev.dres.data.model.run.AbstractInteractiveTask
import dev.dres.data.model.template.TemplateId
import dev.dres.run.score.interfaces.TaskScorer

/**
 * A [Scoreboard] that keeps tracks the total score per team and task group.
 *
 * @author Luca Rossett
 * @version 1.1.0
 */
class SumAggregateScoreBoard(override val name: String, private val boards: List<Scoreboard>, private val taskGroupName: String? = null) : Scoreboard {

    override fun scores(): List<Score> = this.boards.map { it.scores() }
        .flatten().groupBy { it.teamId }.values
        .map { Score(it.first().teamId, it.sumOf { it.score }) }

    override fun score(teamId: TeamId) = boards.sumOf { it.score(teamId) }

    override fun update(runs: List<AbstractInteractiveTask>) {
        //since calls are delegated, nothing needs to be done here
    }

    override fun update(scorers: Map<TemplateId, TaskScorer>) {
        //since calls are delegated, nothing needs to be done here
    }

    override fun overview() = ScoreOverview(this.name, this.taskGroupName, scores())
}