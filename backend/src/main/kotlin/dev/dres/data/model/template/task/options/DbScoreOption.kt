package dev.dres.data.model.template.task.options

import dev.dres.api.rest.types.template.tasks.options.ApiScoreOption
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.XdEnumEntity
import kotlinx.dnq.XdEnumEntityType
import kotlinx.dnq.xdRequiredStringProp

/**
 * An enumeration of potential options for [TaskDescription] scorers.
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
class DbScoreOption(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbScoreOption>() {
        val KIS by enumField { description = "KIS" }
        val AVS by enumField { description = "AVS" }
        val LEGACY_AVS by enumField {description = "LEGACY_AVS"}
        val NOOP by enumField { description = "NOOP" }
    }

    /** Name / description of the [DbScoreOption]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /**
     * Converts this [DbHintOption] to a RESTful API representation [ApiScoreOption].
     *
     * @return [ApiScoreOption]
     */
    fun toApi() = ApiScoreOption.values().find { it.toDb() == this }
        ?: throw IllegalStateException("Option ${this.description} is not supported.")

    override fun toString(): String = this.description
}
