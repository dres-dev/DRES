package dev.dres.api.rest

import io.javalin.openapi.plugin.swagger.SwaggerConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerPlugin

class ClientSwaggerPlugin : SwaggerPlugin(
    SwaggerConfiguration().apply {
        this.documentationPath = "/client-oas"
        this.uiPath = "/swagger-client"
    })