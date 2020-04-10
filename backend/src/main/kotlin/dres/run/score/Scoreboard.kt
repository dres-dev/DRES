package dres.run.score

import dres.data.model.competition.Team

/**
 * Container for [Scoreboard].
 */
data class Score(val teamId: Int, val score: Double)

data class ScoreOverview(val name: String, val scores: List<Score>)

/**
 * A [Scoreboard] tracks the [Score]s for different [Teams]
 *
 * @author Ralph Gasser
 * @version 1.0
 */
interface Scoreboard {

    /**
     * Returns all overall [Score]s tracked by this [Scoreboard].
     */
    fun scores(): List<Score>

    /**
     * Retrieves and returns the score of the given [Team]
     *
     * @param team The [Team]'s ID.
     * @return The score for the given [Team].
     */
    fun score(team: Team): Double

    /**
     * Updates the [Scoreboard].
     */
    fun update()

    /**
     * Returns the name of the [Scoreboard]
     */
    fun name(): String

    /**
     * Returns a summary of all current scores in a [ScoreOverview]
     */
    fun overview(): ScoreOverview = ScoreOverview(name(), scores())

}