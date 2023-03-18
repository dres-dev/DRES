package dev.dres.mgmt.cache

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable

/**
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
}