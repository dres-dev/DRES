package dev.dres.api.rest.types.users

/**
 * A request surrounding manipulation of users.
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
data class UserRequest(val username: String, val password: String?, val role: ApiRole?)