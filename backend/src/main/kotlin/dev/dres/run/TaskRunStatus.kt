package dev.dres.run

enum class TaskRunStatus {

    CREATED,
    /**
     * A [Task] is currently being prepared for execution by the [RunManager].
     *
     * This is an optional [RunManagerStatus] and can or cannot be used by the [RunManager] implementation.
     */
    PREPARING,

    /** A [Task] is currently being executed by the [RunManager]. */
    RUNNING,

    /** A [Task] has been completed by the [RunManager]. */
    ENDED,

}