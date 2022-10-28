package dev.dres.api.rest.types.users

import io.javalin.security.RouteRole

/**
 * An enumeration of all roles exposed through the API.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiRole : RouteRole { ANYONE, VIEWER, PARTICIPANT, JUDGE, ADMIN }