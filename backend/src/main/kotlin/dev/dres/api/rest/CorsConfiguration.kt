package dev.dres.api.rest

import io.javalin.config.JavalinConfig
import org.slf4j.LoggerFactory

/**
 * Installs the cross-origin policy for the API.
 *
 * The frontend is served from the same origin as the API, so a default installation needs no CORS at all
 * and this leaves the plugin disabled. An operator who serves the frontend elsewhere lists the origins it
 * is reachable under and only those are reflected.
 *
 * This used to reflect the caller's own origin while also allowing credentials, which let any page on the
 * internet read authenticated responses - and over plain HTTP (SameSite.LAX, no Secure) let any site a
 * judge or an administrator visited act with that session. Because the rule needs an allowlist to be
 * meaningful, an empty list disables the plugin instead of reflecting everything; Javalin rejects an
 * enabled plugin with no origins and `reflectClientOrigin = false`, which is a second reason for the
 * same layout.
 *
 * Extracted from [RestApi] so that tests can install exactly this rule rather than a copy of it.
 */
object CorsConfiguration {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Applies the policy described by [allowedOrigins] to [config].
     *
     * @param config The Javalin configuration being built.
     * @param allowedOrigins Hosts (optionally with port) whose browser requests may carry credentials.
     */
    fun apply(config: JavalinConfig, allowedOrigins: List<String>) {
        if (allowedOrigins.isEmpty()) {
            logger.info(
                "CORS is disabled. Set cors.allowedOrigins to allow browser clients from other origins. " +
                    "Same-origin clients are unaffected."
            )
            return
        }

        logger.info("CORS allows credentialed requests from: ${allowedOrigins.joinToString()}")
        config.bundledPlugins.enableCors { cors ->
            cors.addRule { rule ->
                rule.reflectClientOrigin = false
                rule.allowCredentials = true
                allowedOrigins.forEach { origin ->
                    rule.allowHost(origin, *listOf("http", "https").toTypedArray())
                }
            }
        }
    }
}
