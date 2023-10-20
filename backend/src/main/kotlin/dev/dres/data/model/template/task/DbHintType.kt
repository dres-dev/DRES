package dev.dres.data.model.template.task

import dev.dres.api.rest.types.template.tasks.ApiHintType
import dev.dres.api.rest.types.task.ApiContentType
import dev.dres.data.model.template.task.options.DbScoreOption
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

class DbHintType(entity: Entity) : XdEnumEntity(entity) {
    companion object : XdEnumEntityType<DbHintType>() {
        val EMPTY by enumField { description = "EMPTY"; base64 = false }
        val TEXT by enumField { description = "TEXT"; mimeType = "text/plain"; suffix = "txt"; base64 = false }
        val VIDEO by enumField { description = "VIDEO"; mimeType = "video/mp4"; suffix = "mp4"; base64 = true }
        val IMAGE by enumField {  description = "IMAGE"; mimeType = "image/jpg"; suffix = "jpg"; base64 = true  }
    }

    /** Name / description of the [DbScoreOption]. */
    var description by xdRequiredStringProp(unique = true)
        private set

    /** Name / description of the [DbScoreOption]. */
    var mimeType by xdStringProp()
        private set

    /** Name / description of the [DbScoreOption]. */
    var suffix by xdStringProp()

    /** Name / description of the [DbScoreOption]. */
    var base64 by xdBooleanProp()

    /**
     * Converts this [DbHintType] to the RESTful API representation [ApiContentType].
     *
     * @return [ApiContentType] equivalent to this [DbHintType].
     */
    fun toApi(): ApiHintType
        = ApiHintType.values().find { it.toDb() == this } ?: throw IllegalStateException("Hint type ${this.description} is not supported.")
}
