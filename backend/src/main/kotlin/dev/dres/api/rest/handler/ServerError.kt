package dev.dres.api.rest.handler

import io.javalin.http.Context
import org.slf4j.LoggerFactory

/**
 * Turns an unexpected server-side failure into a response and a log entry.
 *
 * Before this existed, four handler wrappers, the Javalin exception mapper and the global fallback each
 * invented their own answer, and most of them returned `e.localizedMessage` to the client. Exception
 * messages in this codebase routinely name entities, ids and filesystem paths ("File /srv/data/… not
 * found", "DbUser[24-0] was removed"), so the response body leaked deployment detail to anyone able to
 * provoke a failure.
 *
 * The client gets a generic message; the full stack trace goes to the log, where an operator can see it.
 *
 * @author DRES contributors
 */
object ServerError {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    /** Message returned to clients instead of the internal exception text. */
    const val MESSAGE = "Internal server error."

    /**
     * Logs [error] together with what the caller was doing and answers `500` with [MESSAGE].
     *
     * @param ctx The request context.
     * @param error The exception that escaped a handler.
     */
    fun respond(ctx: Context, error: Throwable) {
        logger.error(
            "Unhandled {} while serving {} {}",
            error.javaClass.simpleName,
            ctx.req().method,
            ctx.path(),
            error
        )
        ctx.status(500)
        ctx.json(dev.dres.api.rest.types.status.ErrorStatus(MESSAGE))
    }
}
