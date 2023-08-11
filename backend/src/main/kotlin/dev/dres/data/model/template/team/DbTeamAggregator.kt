package dev.dres.data.model.template.team

import dev.dres.api.rest.types.template.team.ApiTeamAggregatorType
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * Enumeration of available [DbTeamAggregator]s.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.0.0
 */
class DbTeamAggregator(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbTeamAggregator>() {
        val MAX by enumField { description = "MAX" }
        val MIN by enumField { description = "MIN" }
        val MEAN by enumField { description = "MEAN" }
        val LAST by enumField { description = "LAST" }
    }

    /** Name / description of the [DbTeamAggregator]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    override fun toString() = this.description

    fun toApi(): ApiTeamAggregatorType = ApiTeamAggregatorType.values().find { it.toDb() == this } ?: throw IllegalStateException("TeamAggregator ${this.description} is not supported.")
}
