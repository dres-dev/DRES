package dev.dres.run

import dev.dres.data.model.run.Evaluation
import dev.dres.data.model.run.Task

/**
 * The status of a [Task] within an [Evaluation].
 *
 * @author Luca Rossetto
 * @version 1.0.0.
 */
enum class TaskStatus {
    /**
     * A [Task] was freshly created and is ready for execution.
     */
    CREATED,

    /**
     * A [Task] is currently being prepared for execution.
     */
    PREPARING,

    /** A [Task] is currently being executed. */
    RUNNING,

    /** A [Task] has been completed. */
    ENDED;
}