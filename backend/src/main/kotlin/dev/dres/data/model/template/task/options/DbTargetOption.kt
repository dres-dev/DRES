package dev.dres.data.model.template.task.options

import dev.dres.api.rest.types.competition.tasks.options.ApiTargetOption
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
class DbTargetOption(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbTargetOption>() {
        val MEDIA_ITEM by enumField { description = "MEDIA_ITEM" }
        val MEDIA_SEGMENT by enumField { description = "MEDIA_SEGMENT" }
        val JUDGEMENT by enumField { description = "JUDGEMENT" }
        val VOTE by enumField { description = "VOTE" }
        val TEXT by enumField { description = "TEXT" }
    }

    /** Name / description of the [DbTargetOption]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Converts this [DbHintOption] to a RESTful API representation [ApiTargetOption].
     *
     * @return [ApiTargetOption]
     */
    fun toApi() = ApiTargetOption.values().find { it.toDb() == this } ?: throw IllegalStateException("Option ${this.description} is not supported.")

    override fun toString(): String = this.description
}