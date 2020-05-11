package dres.utilities.extensions

import dres.api.rest.types.status.ErrorStatus
import dres.api.rest.types.status.ErrorStatusException
import dres.api.rest.util.MimeTypeHelper
import io.javalin.http.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

fun Context.errorResponse(status: Int, errorMessage: String) {
    this.status(status)
    this.json(ErrorStatus(errorMessage))
}

fun Context.errorResponse(error: ErrorStatusException) {
    this.status(error.statusCode)
    this.json(error.errorStatus)
}

fun Context.streamFile(file: File) {
    if (!file.exists()){
        this.errorResponse(404, "'${file.name}' not found")
        return
    }
    val mimeType = MimeTypeHelper.mimeType(file)
    this.contentType(mimeType) //needs to be set, probably a bug in Javalin
    this.seekableStream(file.inputStream(), mimeType)
}

fun Context.streamFile(path: Path) {
    if (!Files.exists(path)){
        throw ErrorStatusException(404, "File $path not found!")
    }
    val mimeType = MimeTypeHelper.mimeType(path.toFile())
    this.contentType(mimeType)
    this.seekableStream(Files.newInputStream(path, StandardOpenOption.READ), mimeType)
}

fun Context.sessionId(): String = this.queryParam("session", this.req.session.id)!!