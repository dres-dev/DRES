package dres.api.rest.types.status

import org.slf4j.LoggerFactory

data class ErrorStatusException(val statusCode: Int, val status: String) : Exception(status) {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    init {
        logger.info("ErrorStatusException with code $statusCode and message '$status' thrown by ${stackTrace.first()}")
    }

    val errorStatus: ErrorStatus
        get() = ErrorStatus(status)
}