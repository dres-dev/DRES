package dev.dres.run.score.scoreboard

import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.run.AbstractInteractiveTask
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.interfaces.TaskScorer

/**
 * A [Scoreboard] tracks the [Score]s for different [DbTeam]s
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
     * Retrieves and returns the score of the given [DbTeam]
     *
     * @param teamId The [DbTeam]'s [TeamId].
     * @return The score for the given [DbTeam].
     */
    fun score(teamId: TeamId): Double

    /**
     * Updates the [Scoreboard].
     */
    fun update(runs: List<AbstractInteractiveTask>)

    /**
     * Updates using a map of the [TaskId] ids to the corresponding [TaskScorer]s
     */
    fun update(scorers: Map<TaskId, TaskScorer>)

    /**
     * Returns a summary of all current scores in a [ScoreOverview]
     */
    fun overview(): ScoreOverview
}