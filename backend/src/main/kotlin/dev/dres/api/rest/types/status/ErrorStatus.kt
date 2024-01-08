package dev.dres.api.rest.types.status

import kotlinx.serialization.Serializable

@Serializable
data class ErrorStatus(val description: String): AbstractStatus(status = false)