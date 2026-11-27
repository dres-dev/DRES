package dev.dres.api.rest.handler.collection

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Streams an external media upload to disk under a hard size bound.
 *
 * The upload endpoint used to call `ctx.bodyAsBytes()`, so a 20 MB body became a 20 MB array per
 * concurrent request and the whole body had to arrive before anything was written. Streaming keeps
 * the footprint at one copy buffer, and lets an oversized body be refused while it is still
 * arriving instead of after the server has already buffered all of it.
 *
 * Writes go to a scratch file in the same directory as the target and are moved into place only
 * once the body is complete, so an interrupted upload cannot leave a truncated file that later
 * looks like valid media. A scratch file never survives this object, whatever happens.
 */
object ExternalUpload {

    /**
     * Copies [body] into [target] and returns the number of bytes written.
     *
     * @param target destination file; its parent directory must already exist
     * @param body the request body, read to its end or until [maxBytes] is exceeded
     * @param maxBytes hard upper bound on the accepted size
     * @throws UploadTooLargeException when the body exceeds [maxBytes]
     * @throws EmptyUploadException when the body carries no bytes
     */
    fun receive(target: Path, body: InputStream, maxBytes: Long = MAX_BYTES): Long {
        val scratch = Files.createTempFile(target.parent, "${target.fileName}.", ".part")
        try {
            var total = 0L
            val buffer = ByteArray(COPY_BUFFER_BYTES)
            Files.newOutputStream(scratch).use { sink ->
                while (true) {
                    val read = body.read(buffer)
                    if (read < 0) {
                        break
                    }
                    total += read
                    if (total > maxBytes) {
                        throw UploadTooLargeException(maxBytes)
                    }
                    sink.write(buffer, 0, read)
                }
            }
            if (total == 0L) {
                throw EmptyUploadException()
            }
            Files.move(scratch, target, StandardCopyOption.REPLACE_EXISTING)
            return total
        } finally {
            Files.deleteIfExists(scratch)
        }
    }

    /** Hard bound on an uploaded external file, matching the server-wide request limit. */
    const val MAX_BYTES: Long = 20L * 1024L * 1024L

    private const val COPY_BUFFER_BYTES = 64 * 1024
}

/** Signals a body that outgrew the accepted size. */
class UploadTooLargeException(val maxBytes: Long) : Exception("The upload exceeds the maximum size of $maxBytes bytes.")

/** Signals a body that carried no bytes. */
class EmptyUploadException : Exception("The upload is empty.")
