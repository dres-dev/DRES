package dev.dres.data.model.competition.team

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * Enumeration of available [TeamAggregator]s.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.0
 */
class TeamAggregator(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<TeamAggregator>() {
        val MAX by enumField { description = "MAX" }
        val MIN by enumField { description = "MIN" }
        val MEAN by enumField { description = "MEAN" }
        val LAST by enumField { description = "LAST" }
    }

    /** Name / description of the [TeamAggregator]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Creates and returns a new [TeamAggregatorImpl] for this [TeamAggregator].
     *
     * @param teams The list of [Team]s to create the [TeamAggregatorImpl] for.
     * @return [TeamAggregatorImpl]
     */
    fun newInstance(teams: List<Team>) = when(this) {
        MAX -> TeamAggregatorImpl.Max(teams)
        MIN -> TeamAggregatorImpl.Min(teams)
        MEAN -> TeamAggregatorImpl.Mean(teams)
        LAST -> TeamAggregatorImpl.Last(teams)
        else -> throw IllegalStateException("Failed to generated aggregator for unknown team group ${this.description}.")
    }
}