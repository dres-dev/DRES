package dev.dres.api.rest

import io.javalin.plugin.openapi.annotations.HttpMethod

/**
 * Options to configure an OpenApi Specifications endpoint in use with Javalin OpenApi Plugin.
 */
data class OpenApiEndpointOptions(
        /**
         * The path of the OpenApi Specifications JSON
         */
        val oasPath: String,
        /**
         * The path of the swagger UI
         */
        val swaggerUi: String,
        /**
         * Optionally a path for the redoc UI - if omitted, no redoc UI should be made available
         */
        val redocUi: String?,
        /**
         * A list of paths to ignore. Defaults to an empty list
         */
        private val ignores:List<String> = listOf()
) {

    val ignored = ignores.map { "/api$it" to HttpMethod.values().map { it } }
    val hasRedoc = redocUi != null

    companion object {
        val dresDefaultOptions = OpenApiEndpointOptions("/swagger-doc", "/swagger-ui", "/redoc")
        val dresLogOnly = OpenApiEndpointOptions("/logging-oas", "/swagger-log",
                ignores=listOf("/login", "/logout", "/user*", "/collection*", "/competition*", "/run*", "/submit"))
        val dresSubmissionOnly = OpenApiEndpointOptions("/logging-oas", "/swagger-log",
                ignores=listOf("/login", "/logout", "/user*", "/collection*", "/competition*", "/run*", "/log*"))
    }

    constructor(oasPath: String, swaggerUi: String, ignores: List<String>) : this(oasPath, swaggerUi, null, ignores)
}