package dres.api.rest.types.status

import java.lang.Exception

data class ErrorStatusException(val statusCode: Int, val status: String) : Exception(status) {
    val errorStatus: ErrorStatus
        get() = ErrorStatus(status)
}