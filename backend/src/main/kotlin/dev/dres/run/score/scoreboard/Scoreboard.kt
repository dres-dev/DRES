package dev.dres.run.score.scoreboard

import dev.dres.data.model.UID
import dev.dres.data.model.competition.Team
import dev.dres.data.model.competition.TeamId
import dev.dres.data.model.run.CompetitionRun
import dev.dres.data.model.run.TaskRunId
import dev.dres.run.score.interfaces.TaskRunScorer

/**
 * Container for [Scoreboard].
 */
data class Score(val teamId: String, val score: Double)

data class ScoreOverview(val name: String, val taskGroup: String?, val scores: List<Score>)

/**
 * A [Scoreboard] tracks the [Score]s for different [Team]s
 *
 * @author Ralph Gasser
 * @version 1.0.1
 */
interface Scoreboard {

    /**
     * Returns the name of the [Scoreboard]
     */
    val name: String

    /**
     * Returns all overall [Score]s tracked by this [Scoreboard].
     */
    fun scores(): List<Score>

    /**
     * Retrieves and returns the score of the given [Team] [UID]
     *
     * @param teamId The [Team]'s [UID].
     * @return The score for the given [Team].
     */
    fun score(teamId: TeamId): Double

    /**
     * Updates the [Scoreboard].
     */
    fun update(runs: List<CompetitionRun.TaskRun>)

    /**
     * Updates using a map of the [TaskRun] ids to the corresponding [TaskRunScorer]s
     */
    fun update(scorers: Map<TaskRunId, TaskRunScorer>)

    /**
     * Returns a summary of all current scores in a [ScoreOverview]
     */
    fun overview(): ScoreOverview

}