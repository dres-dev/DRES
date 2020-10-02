package dev.dres.api.rest.types.status

import io.javalin.http.Context
import org.slf4j.LoggerFactory

data class ErrorStatusException(val statusCode: Int, val status: String, private val ctx: Context, val doNotLog: Boolean = false) : Exception(status) {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    init {
        if(!doNotLog){
            logger.info("ErrorStatusException with code $statusCode and message '$status' thrown by ${stackTrace.first()} for request from ${ctx.req.remoteAddr}")
        }
    }

    val errorStatus: ErrorStatus
        get() = ErrorStatus(status)
}