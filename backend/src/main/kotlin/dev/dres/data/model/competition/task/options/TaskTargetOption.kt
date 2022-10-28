package dev.dres.data.model.competition.task.options

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp
/**
 * An enumeration of potential options for [TaskDescription] targets.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class TaskTargetOption(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<TaskTargetOption>() {
        val SINGLE_MEDIA_ITEM by enumField { description = "SINGLE_MEDIA_ITEM" }
        val SINGLE_MEDIA_SEGMENT by enumField { description = "SINGLE_MEDIA_SEGMENT" }
        val MULTIPLE_MEDIA_ITEMS by enumField { description = "MULTIPLE_MEDIA_ITEMS" }
        val JUDGEMENT by enumField { description = "JUDGEMENT" }
        val VOTE by enumField { description = "VOTE" }
        val TEXT by enumField { description = "TEXT" }

    }

    /** Name / description of the [TaskTargetOption]. */
    var description by xdRequiredStringProp(unique = true)
        private set
}