package dev.dres.data.model.template.task.options

import dev.dres.data.model.template.task.DbTaskTemplate
import dev.dres.data.model.template.task.DbTaskType
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

/**
 * A helper class that can be used to configure options in a [DbTaskType].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DbConfiguredOption(entity: Entity) : XdEntity(entity) {

    companion object: XdNaturalEntityType<DbConfiguredOption>()

    /** The key for this [DbConfiguredOption]. Identifies the option. */
    var key by xdRequiredStringProp()

    /** The conifgured value for this [DbConfiguredOption]. */
    var value by xdRequiredStringProp()

    /** The [DbTaskTemplate] this [DbConfiguredOption] belongs to. */
    val task: DbTaskType by xdParent<DbConfiguredOption,DbTaskType>(DbTaskType::configurations)

    /**
     * Tries to parse a named parameter as [Boolean]. Returns null, if the parameter is not set or cannot be converted.
     *
     * @return [Boolean] or null.
     */
    fun getAsBool(): Boolean? = this.value.toBooleanStrictOrNull()

    /**
     * Tries to parse a named parameter as [Int]. Returns null, if the parameter is not set or cannot be converted.
     *
     * @return [Int] or null.
     */
    fun getAsInt(): Int? = this.value.toIntOrNull()

    /**
     * Tries to parse a named parameter as [Long]. Returns null, if the parameter is not set or cannot be converted.
     *
     * @return [Long] or null.
     */
    fun getAsLong(): Long? = this.value.toLongOrNull()

    /**
     * Tries to parse a named parameter as [Float]. Returns null, if the parameter is not set or cannot be converted.
     *
     * @return [Float] or null.
     */
    fun getAsFloat(): Float? = this.value.toFloatOrNull()

    /**
     * Tries to parse a named parameter as [Double]. Returns null, if the parameter is not set or cannot be converted.
     *
     * @return [Double] or null.
     */
    fun getAsDouble(): Double? = this.value.toDoubleOrNull()
}
