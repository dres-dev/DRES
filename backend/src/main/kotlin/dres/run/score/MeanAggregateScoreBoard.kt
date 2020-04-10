package dres.run.score

import dres.data.model.competition.Team

class MeanAggregateScoreBoard(private val name: String, private val boards: List<Scoreboard>) : Scoreboard {

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

    override fun score(team: Team) = boards.map { it.score(team) }.sum() / boards.size


    override fun update() {
        //since calls are delegated, nothing needs to be done here
    }

    override fun name() = name

}