package dev.dres.data.model.competition.options

/**
 * A concrete instance for an [Option] including named paramters.
 *
 * @author Luca Rossetto & Ralph Gasser
 * @version 1.1.0
 */
data class ConfiguredOption<T: Option>(val option: T, val parameters : Map<String, String> = emptyMap()) {
    /**
     * Tries to access the  named parameter. Returns null, if the parameter is not set.
     *
     * @param name Name of the parameter to return.
     * @return [String] or null.
     */
    operator fun get(name: String): String? = this.parameters[name]

    /**
     * Tries to parse a named parameter as [Int]. Returns null, if the parameter is not set or cannot be converted.
     *
     * @param name Name of the parameter to return.
     * @return [Int] or null.
     */
    fun getAsInt(name: String): Int? = this.parameters[name]?.toIntOrNull()

    /**
     * Tries to parse a named parameter as [Long]. Returns null, if the parameter is not set or cannot be converted.
     *
     * @param name Name of the parameter to return.
     * @return [Long] or null.
     */
    fun getAsLong(name: String): Long? = this.parameters[name]?.toLongOrNull()

    /**
     * Tries to parse a named parameter as [Float]. Returns null, if the parameter is not set or cannot be converted.
     *
     * @param name Name of the parameter to return.
     * @return [Float] or null.
     */
    fun getAsFloat(name: String): Float? = this.parameters[name]?.toFloatOrNull()

    /**
     * Tries to parse a named parameter as [Double]. Returns null, if the parameter is not set or cannot be converted.
     *
     * @param name Name of the parameter to return.
     * @return [Double] or null.
     */
    fun getAsDouble(name: String): Double? = this.parameters[name]?.toDoubleOrNull()
}