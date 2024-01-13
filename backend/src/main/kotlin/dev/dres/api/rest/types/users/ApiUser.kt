package dev.dres.api.rest.types.users

import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.admin.UserId
import kotlinx.serialization.Serializable

/**
 * A RESTful API representation of a [DbUser]
 *
 * @author Loris Sauter
 * @version 1.0.0
 */
@Serializable
data class ApiUser(val id: UserId? = null, val username: String? = null, val role: ApiRole? = null, var sessionId: String? = null)