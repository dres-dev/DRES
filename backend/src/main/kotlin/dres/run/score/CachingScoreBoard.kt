package dres.run.score

import dres.data.model.competition.Team

class CachingScoreBoard(private val board: Scoreboard): Scoreboard {

    private var taskScores: List<Score> = board.taskScores()
    private var overallScores: List<Score> = board.overallScores()
    private val taskScoreMap = mutableMapOf<Team, Double>()

    override fun taskScores() = taskScores

    override fun overallScores() = overallScores

    override fun taskScore(team: Team): Double {
        if (!taskScoreMap.containsKey(team)){
            taskScoreMap.put(team, board.taskScore(team))
        }
        return taskScoreMap[team]!!
    }

    override fun update() {
        taskScores = board.taskScores()
        overallScores = board.overallScores()
        taskScoreMap.clear()
    }

    override fun name() = board.name()
}