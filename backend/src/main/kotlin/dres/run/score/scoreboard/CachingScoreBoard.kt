package dres.run.score.scoreboard

import dres.data.model.run.CompetitionRun

class CachingScoreBoard(private val board: Scoreboard): Scoreboard {

    //private var taskScores: List<Score> = board.taskScores()
    private var overallScores: List<Score> = board.scores()
    private val taskScoreMap = mutableMapOf<Int, Double>()

    //override fun taskScores() = taskScores

    override fun scores() = overallScores

    override fun score(teamId: Int): Double {
        if (!taskScoreMap.containsKey(teamId)){
            taskScoreMap[teamId] = board.score(teamId)
        }
        return taskScoreMap[teamId]!!
    }

    override fun update(runs: List<CompetitionRun.TaskRun>) {
        //taskScores = board.taskScores()
        overallScores = board.scores()
        taskScoreMap.clear()
    }

    override fun name() = board.name()
}