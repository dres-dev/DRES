package dev.dres.data.model.template.task.options

import dev.dres.data.model.template.task.TaskTemplate
import dev.dres.data.model.template.task.TaskType
import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*

/**
 * A helper class that can be used to configure options in a [TaskType].
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class ConfiguredOption(entity: Entity) : XdEntity(entity) {

    companion object: XdNaturalEntityType<ConfiguredOption>()

    /** The key for this [ConfiguredOption]. Identifies the option. */
    var key by xdRequiredStringProp()

    /** The conifgured value for this [ConfiguredOption]. */
    var value by xdRequiredStringProp()

    /** The [TaskTemplate] this [ConfiguredOption] belongs to. */
    val task: TaskType by xdParent<ConfiguredOption,TaskType>(TaskType::configurations)

    /**
     * Tries to parse a named parameter as [Boolean]. Returns null, if the parameter is not set or cannot be converted.
     *
     * @param name Name of the parameter to return.
     * @return [Boolean] or null.
     */
    fun getAsBool(name: String): Boolean? = this.value.toBooleanStrictOrNull()

    /**
     * Tries to parse a named parameter as [Int]. Returns null, if the parameter is not set or cannot be converted.
     *
     * @param name Name of the parameter to return.
     * @return [Int] or null.
     */
    fun getAsInt(name: String): Int? = this.value.toIntOrNull()

    /**
     * Tries to parse a named parameter as [Long]. Returns null, if the parameter is not set or cannot be converted.
     *
     * @param name Name of the parameter to return.
     * @return [Long] or null.
     */
    fun getAsLong(name: String): Long? = this.value.toLongOrNull()

    /**
     * Tries to parse a named parameter as [Float]. Returns null, if the parameter is not set or cannot be converted.
     *
     * @param name Name of the parameter to return.
     * @return [Float] or null.
     */
    fun getAsFloat(name: String): Float? = this.value.toFloatOrNull()

    /**
     * Tries to parse a named parameter as [Double]. Returns null, if the parameter is not set or cannot be converted.
     *
     * @param name Name of the parameter to return.
     * @return [Double] or null.
     */
    fun getAsDouble(name: String): Double? = this.value.toDoubleOrNull()
}