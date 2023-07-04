package dev.dres.api.rest

import io.javalin.Javalin
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerHandler
import io.javalin.openapi.plugin.swagger.SwaggerPlugin
import io.javalin.plugin.Plugin

class ClientSwaggerPlugin : Plugin {
    override fun apply(app: Javalin) {
        val swaggerHandler = SwaggerHandler(
            title = "DRES Client API",
            documentationPath = "/client-oas",
            swaggerVersion = SwaggerConfiguration().version,
            validatorUrl = "https://validator.swagger.io/validator",
            routingPath = app.cfg.routing.contextPath,
            basePath = null,
            tagsSorter = "'alpha'",
            operationsSorter = "'alpha'",
            customJavaScriptFiles = emptyList(),
            customStylesheetFiles = emptyList()
        )

        app.get("/swagger-client", swaggerHandler)
    }

}
