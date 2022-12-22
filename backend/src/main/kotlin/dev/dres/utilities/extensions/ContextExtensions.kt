package dev.dres.utilities.extensions

import dev.dres.api.rest.AccessManager
import dev.dres.api.rest.types.status.ErrorStatus
import dev.dres.api.rest.types.status.ErrorStatusException
import dev.dres.api.rest.util.MimeTypeHelper
import io.javalin.http.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.random.Random

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
    this.writeSeekableStream(file.inputStream(), mimeType)
}

fun Context.streamFile(path: Path) {
    if (!Files.exists(path)){
        this.errorResponse(404, "File $path not found!")
        return
    }
    val mimeType = MimeTypeHelper.mimeType(path.toFile())
    this.contentType(mimeType)
    this.writeSeekableStream(Files.newInputStream(path, StandardOpenOption.READ), mimeType)
}

fun Context.sendFile(file: File) {
    if (!file.exists()){
        this.errorResponse(404, "'${file.name}' not found")
        return
    }
    val mimeType = MimeTypeHelper.mimeType(file)
    this.contentType(mimeType)
    this.result(file.inputStream())
}

fun Context.sessionToken(): String? = this.attribute<String>("session")

fun Context.getOrCreateSessionToken(): String {
    val attributeId = this.attribute<String>("session")
    if (attributeId != null) {
        return attributeId
    }

    val random = Random(System.nanoTime())
    val id = List(AccessManager.SESSION_TOKEN_LENGTH) { AccessManager.SESSION_TOKEN_CHAR_POOL.random(random) }.joinToString("")

    this.attribute("session", id)

    return id

}