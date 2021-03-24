package dev.dres.run.exceptions

import dev.dres.run.RunManager
import dev.dres.run.RunManagerStatus

/**
 * An [IllegalStateException] that gets thrown whenever a [RunManager] is not in the right [RunManagerStatus] to execute a command.
 * Errors like this are usually linked to bad user input.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class IllegalRunStateException(status: RunManagerStatus) : IllegalStateException("Could not execute request because run manager is in wrong state (s = $status).")
