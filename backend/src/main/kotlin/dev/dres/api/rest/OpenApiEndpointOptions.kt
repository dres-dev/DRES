package dev.dres.api.rest

import io.javalin.openapi.HttpMethod


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
     * A list of paths to ignore. Defaults to an empty list
     */
    private val ignores: List<String> = listOf()
) {

    fun ignored(): List<Pair<String, List<HttpMethod>>> {
        return _ignored
    }

    private val _ignored = ignores.map {
        /* Small routine to distinguish between "internal" (prefixed with /api/) and "public" endpoints */
        if(!it.startsWith("#")){
            "/api/v1$it" to HttpMethod.values().map { it } //FIXME deal with version number
        }else{
            it.substring(it.indexOf("#")+1) to HttpMethod.values().map { it }
        }
    }.toMutableList()

    fun withIgnores(ignores: List<Pair<String, List<HttpMethod>>>) : OpenApiEndpointOptions {
        this._ignored.addAll(ignores)
        return this
    }

    companion object {
        val commonIgnores = mutableListOf(
            "/external/*",
            "/collection*", "/collection/*",
            "/competition*", "/competition/*",
            "/run*", "/run/*",
            "/audit*", "/audit/*",
            "/mediaItem*", "/mediaItem/*",
            "/score*", "/score/*"
        )

        val dresDefaultOptions = OpenApiEndpointOptions("/swagger-docs", "/swagger-ui")

        val dresSubmittingClientOptions = OpenApiEndpointOptions(
            "/client-oas",
            "/swagger-client",
            ignores= commonIgnores +
                    listOf(
                        "/user/list",
                        "/user/session/*"
                    )
        ).withIgnores(listOf(
            "/api/v1/user" to HttpMethod.values().map { it }.filter { it.ordinal != HttpMethod.GET.ordinal },
            "/api/v1/user/{userId}" to HttpMethod.values().map{it}
        ))
    }
}
