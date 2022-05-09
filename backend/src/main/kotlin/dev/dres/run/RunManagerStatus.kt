package dev.dres.run

/**
 * The collection auf statuses a [RunManager] can be in.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
enum class RunManagerStatus {
    /** [RunManager] was created and has not started yet. It can now be setup. */
    CREATED,

    /** [RunManager] was started and setup; it is ready to run [Task]s. */
    ACTIVE,

    /** [RunManager] was terminated and cannot run anymore [Task]s. */
    TERMINATED
}