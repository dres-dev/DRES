package dres.api.rest.types.status

data class ErrorStatus(val description: String): Throwable(description)