package dres.run

import dres.data.model.competition.Team

/**
 * Type alias for [Scoreboard].
 */
typealias Score = Pair<Team,Double>

/**
 * A [Scoreboard] tracks the [Score]s for different [Teams]
 *
 * @author Ralph Gasser
 * @version 1.0
 */
interface Scoreboard {
    /**
     * Returns all [Score]s tracked by this [Scoreboard].
     */
    fun all(): List<Score>

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
}