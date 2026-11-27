package dev.dres.run.exceptions

/**
 * Signals that an operation that needs a task deadline was requested for a task that runs perpetually.
 *
 * A perpetual task has no duration, so it can be neither prolonged nor shortened; it ends by submission
 * or by skipping. This is deliberately its own type rather than a bare [IllegalStateException]: the REST
 * layer maps [IllegalArgumentException] to "the new duration went below zero, so the task was stopped" and
 * every other [IllegalStateException] to "the run is in the wrong state", and neither answer is true here.
 *
 * @author Luca Rossetto
 * @version 1.0
 */
class PerpetualTaskException(message: String) : IllegalStateException(message)
