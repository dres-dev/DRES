package dev.dres.mgmt.cache

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.Callable

/**
 * Base class of the cache's rendering jobs.
 *
 * A job writes to a temporary file next to its [output] and publishes it with an atomic move, so a
 * concurrent reader can never observe a half-written preview. Before this, every request wrote straight
 * into [output]: readers that polled `Files.exists` (the preview handlers, the hint and target handlers,
 * other jobs' "is it cached yet" check) could be handed a truncated or zero-byte file that looked like a
 * finished one, and it then stayed cached as such.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
abstract class AbstractPreviewRequest(protected val input: Path, protected val output: Path): Callable<Path> {

    init {
        require(Files.exists(this.input)) { "Could not generate preview because file $input does not exist." }
        if (!Files.exists(output.parent)) {
            Files.createDirectories(output.parent)
        }
    }

    /**
     * Writes this job's result without ever exposing a partial file at [output].
     *
     * [writeTo] receives a scratch path in the same directory, so the later move stays on one file system
     * and can be atomic. If the write throws, the scratch file is removed and [output] is left untouched;
     * a failed regeneration therefore keeps the last good preview instead of replacing it with a broken one.
     *
     * @param writeTo Consumes the scratch [Path] and must produce the finished file there.
     * @return [output], once the result is in place.
     */
    protected fun publish(writeTo: (Path) -> Unit): Path {
        val scratch = Files.createTempFile(
            this.output.parent,
            "${this.output.fileName}.",
            ".part"
        )
        try {
            writeTo(scratch)
            if (Files.size(scratch) == 0L) {
                throw java.io.IOException("Rendering ${this.input.fileName} produced an empty file.")
            }
            /* ATOMIC_MOVE makes the result appear complete or not at all; fall back when unsupported. */
            try {
                Files.move(scratch, this.output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(scratch, this.output, StandardCopyOption.REPLACE_EXISTING)
            }
            return this.output
        } catch (e: Throwable) {
            Files.deleteIfExists(scratch)
            throw e
        }
    }
}
