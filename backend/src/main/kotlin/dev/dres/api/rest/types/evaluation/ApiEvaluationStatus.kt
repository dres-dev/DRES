package dev.dres.api.rest.types.evaluation

import dev.dres.run.RunManager
import dev.dres.run.RunManagerStatus

/**
 * The collection auf statuses a [RunManager] can be in. API version of [RunManagerStatus]
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class ApiEvaluationStatus {
    /** [RunManager] was created and has not started yet. It can now be setup. */
    CREATED,

    /** [RunManager] was started and setup; it is ready to run [Task]s. */
    ACTIVE,

    /** [RunManager] was terminated and cannot run anymore [Task]s. */
    TERMINATED;

    /**
     * Converts this [ApiEvaluationStatus] to the corresponding [RunManagerStatus].
     *
     * @return [RunManagerStatus]
     */
    fun asRunManagerStatus(): RunManagerStatus = when(this) {
        CREATED -> RunManagerStatus.CREATED
        ACTIVE -> RunManagerStatus.ACTIVE
        TERMINATED -> RunManagerStatus.TERMINATED
    }
}