package dev.dres.api.rest.types.template.team

import dev.dres.data.model.template.team.DbTeamAggregator
import dev.dres.data.model.template.team.TeamAggregatorImpl
import dev.dres.data.model.template.team.TeamId

/**
 * The RESTful API equivalent ofr [DbTeamAggregator].
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
enum class ApiTeamAggregatorType {

    MIN, MAX, MEAN, LAST;


    /**
     * Converts this [ApiTeamAggregatorType] to a [DbTeamAggregator] representation. Requires an ongoing transaction.
     *
     * @return [DbTeamAggregator]
     */
    fun toDb(): DbTeamAggregator = when(this) {
        MIN -> DbTeamAggregator.MIN
        MAX -> DbTeamAggregator.MAX
        MEAN -> DbTeamAggregator.MEAN
        LAST -> DbTeamAggregator.LAST
    }

    /**
     * Creates and returns a new [TeamAggregatorImpl] for this [DbTeamAggregator].
     *
     * @param teamIds The list of [TeamId]s to create the [TeamAggregatorImpl] for.
     * @return [TeamAggregatorImpl]
     */
    fun newInstance(teamIds: Set<TeamId>) = when(this) {
        MAX -> TeamAggregatorImpl.Max(teamIds)
        MIN -> TeamAggregatorImpl.Min(teamIds)
        MEAN -> TeamAggregatorImpl.Mean(teamIds)
        LAST -> TeamAggregatorImpl.Last(teamIds)
    }

}
