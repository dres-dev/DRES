package dres.utilities.extensions

import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.util.MimeTypeHelper
import io.javalin.http.Context
import java.io.File

fun Context.errorResponse(status: Int, errorMessage: String) {
    this.status(status)
    this.json(ErrorStatus(errorMessage))
}

fun Context.errorResponse(error: ErrorStatusException) {
    this.status(error.statusCode)
    this.json(error.errorStatus)
}

fun Context.streamFile(file: File){

    if (!file.exists()){
        this.errorResponse(404, "'${file.name}' not found")
        return
    }
    val mimeType = MimeTypeHelper.mimeType(file)
    this.contentType(mimeType) //needs to be set, probably a bug in Javalin
    this.seekableStream(file.inputStream(), mimeType)
}

fun Context.sessionId(): String = this.queryParam("session", this.req.session.id)!!