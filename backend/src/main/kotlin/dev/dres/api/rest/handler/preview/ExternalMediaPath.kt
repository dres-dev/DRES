package dev.dres.api.rest.handler.preview

import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.Files
import java.nio.file.Path

/** Resolves an external media file only when its real path remains below the configured root. */
object ExternalMediaPath {
    fun resolve(root: Path, file: String): Path? = try {
        val realRoot = root.toRealPath()
        val requestedPath = Path.of(file)
        if (requestedPath.isAbsolute) {
            return null
        }
        val candidate = realRoot.resolve(requestedPath).normalize()
        if (!candidate.startsWith(realRoot) || candidate == realRoot || !Files.exists(candidate)) {
            return null
        }

        val realFile = candidate.toRealPath()
        if (!realFile.startsWith(realRoot) || realFile == realRoot || !Files.isRegularFile(realFile)) {
            null
        } else {
            realFile
        }
    } catch (_: InvalidPathException) {
        null
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}
