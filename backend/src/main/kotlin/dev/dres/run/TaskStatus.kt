package dev.dres.run

import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.run.DbTask

/**
 * The status of a [DbTask] within an [DbEvaluation].
 *
 * @author Luca Rossetto
 * @version 1.0.0.
 */
enum class TaskStatus {
    /**
     * A [DbTask] was freshly created and is ready for execution.
     */
    CREATED,

    /**
     * A [DbTask] is currently being prepared for execution.
     */
    PREPARING,

    /** A [DbTask] is currently being executed. */
    RUNNING,

    /** A [DbTask] has been completed. */
    ENDED;
}