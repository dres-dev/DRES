package dev.dres.run.score.scoreboard

import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.run.AbstractInteractiveTask
import dev.dres.data.model.run.interfaces.EvaluationRun
import dev.dres.data.model.run.interfaces.TaskId
import dev.dres.data.model.template.team.TeamId
import dev.dres.run.score.scorer.TaskScorer

/**
 * A [Scoreboard] tracks the [Score]s for different [DbTeam]s
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
interface Scoreboard {

    /** The [EvaluationRun] this [Scoreboard] instance belongs to. */
    val run: EvaluationRun

    /** * Returns the name of the [Scoreboard] */
    val name: String

    /** Flag indicating, that this [Scoreboard] is dirty and needs re-calculation. */
    val dirty: Boolean

    /**
     * Returns all overall [Score]s tracked by this [Scoreboard].
     */
    fun scores(): List<Score>

    /**
     * Retrieves and returns the score of the given [TeamId]
     *
     * @param teamId The [TeamId] to retrieve the score for.
     * @return The score for the given [TeamId].
     */
    fun score(teamId: TeamId): Double

    /**
     * Returns a summary of all current scores in a [ScoreOverview]
     */
    fun overview(): ScoreOverview

    /**
     * Invalidates this [Scoreboard] and marks its content as dirty.
     */
    fun invalidate()
}