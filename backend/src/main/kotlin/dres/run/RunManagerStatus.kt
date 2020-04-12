package dres.run

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

    /**
     * A [Task] is currently being prepared for execution by this [RunManager].
     *
     * This is an optional [RunManagerStatus] and can or cannot be used by a [RunManager] implementation.
     */
    PREPARING_TASK,

    /** A [Task] is currently being executed by this [RunManager]. */
    RUNNING_TASK,

    /** A [Task] has been executed by this [RunManager] but not all processing has completed yet
     * . */
    FINALIZING_TASK,

    /** [RunManager] was terminated and cannot run anymore [Task]s. */
    TERMINATED
}