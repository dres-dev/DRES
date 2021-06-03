package dev.dres.api.rest.types.status

data class ErrorStatus(val description: String): AbstractStatus(status = false)