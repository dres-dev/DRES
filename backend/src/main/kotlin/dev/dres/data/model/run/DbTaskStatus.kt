package dev.dres.data.model.run

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
* Enumeration of the status of a [DbTask].
*
* @author Ralph Gasser
* @version 1.0.0
*/
class DbTaskStatus (entity: Entity): XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbTaskStatus>() {
        val CREATED by DbTaskStatus.enumField { description = "CREATED" }
        val PREPARING by DbTaskStatus.enumField { description = "PREPARING" }
        val RUNNING by DbTaskStatus.enumField { description = "RUNNING" }
        val ENDED by DbTaskStatus.enumField { description = "ENDED" }
        val IGNORED by DbTaskStatus.enumField { description = "IGNORED" }
    }

    var description by xdRequiredStringProp(unique = true)
        private set
}