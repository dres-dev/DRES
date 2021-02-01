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
    private val ignores: List<String> = listOf()
) {

    val ignored = ignores.map {
        /* Small routine to distinguish between "internal" (prefixed with /api/) and "public" endpoints */
        if(!it.startsWith("#")){
            "/api$it" to HttpMethod.values().map { it }
        }else{
            it.substring(it.indexOf("#")+1) to HttpMethod.values().map { it }
        }
    }
    val hasRedoc = redocUi != null


    companion object {
        val commonIgnores = mutableListOf(
             "/external/*",
            "/user*",
            "/collection*", "/collection/*",
            "/competition*", "/competition/*",
            "/run*", "/run/*",
            "/audit*", "/audit/*",
            "/mediaItem*", "/mediaItem/*",
            "/score*", "/score/*"
        )
        val lessCommonIgnores = listOf(
            "/login", "/logout", "/status/*", "/user/*"
        )
        val dresDefaultOptions = OpenApiEndpointOptions("/swagger-docs", "/swagger-ui", "/redoc")
        val dresLogOnly = OpenApiEndpointOptions("/logging-oas", "/swagger-log",
            ignores = commonIgnores +  listOf("#/submit"))
        val dresSubmissionOnly = OpenApiEndpointOptions("/submission-oas", "/swagger-submit",
            ignores = commonIgnores + listOf("#/log*", "#/log/*") )

        val dresSubmittingClientOptions = OpenApiEndpointOptions("/client-oas", "/swagger-client", ignores= commonIgnores + listOf("/user/list", "/user/session/*"))
    }

    constructor(oasPath: String, swaggerUi: String, ignores: List<String>) : this(
        oasPath,
        swaggerUi,
        null,
        ignores
    )
}