package dev.dres.data.model.competition.team

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 *
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
}