package dev.dres.api.rest.types.template.team

import dev.dres.data.model.template.team.DbTeamAggregator

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

}
