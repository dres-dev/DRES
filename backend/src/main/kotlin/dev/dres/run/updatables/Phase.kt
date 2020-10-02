package dev.dres.run.updatables

/**
 * Abstraction of a [Phase] within a [RunManager]'s run loop. Each iteration undergoes the [Phase]s
 * in the order [PREPARE], [MAIN] and [FINALIZE].
 *
 * @author Ralph Gasser
 * @version 1.0
 */
enum class Phase {
    PREPARE, /** Preparation [Phase] in the run loop. Used to build data structures or prepare data. */
    MAIN, /** Main [Phase] in the run loop. Used to update internal state e.g. recalculate scoreboards. */
    FINALIZE  /** Finalization [Phase] in the run loop. Used to send out messages or persist information. */
}