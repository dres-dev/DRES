package dev.dres.run.score.scoreboard

import dev.dres.data.model.run.CompetitionRun

class MeanAggregateScoreBoard(private val name: String, private val boards: List<Scoreboard>, private val taskGroupName: String? = null) : Scoreboard {

//    override fun taskScores(): List<Score> {
//        return boards.map { it.taskScores() }
//                .flatten().groupBy { it.teamId }.values
//                .map { Score(it.first().teamId, it.map { it.score }.sum() / boards.size) }
//    }

    override fun scores(): List<Score> {
        return boards.map { it.scores() }
                .flatten().groupBy { it.teamId }.values
                .map { Score(it.first().teamId, it.map { it.score }.sum() / boards.size) }
    }

    override fun score(teamId: Int) = boards.map { it.score(teamId) }.sum() / boards.size


    override fun update(runs: List<CompetitionRun.TaskRun>) {
        //since calls are delegated, nothing needs to be done here
    }

    override fun name() = name

    override fun overview() = ScoreOverview(name, taskGroupName, scores())

}