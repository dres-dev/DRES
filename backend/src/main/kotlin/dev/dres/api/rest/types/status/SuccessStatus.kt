package dev.dres.api.rest.types.status

import kotlinx.serialization.Serializable

@Serializable
data class SuccessStatus(val description: String): AbstractStatus(status = true)