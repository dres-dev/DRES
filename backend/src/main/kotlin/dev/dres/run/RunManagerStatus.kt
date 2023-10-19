package dev.dres.run

import dev.dres.api.rest.types.evaluation.ApiEvaluationStatus

/**
 * The collection auf statuses a [RunManager] can be in.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class RunManagerStatus {
    /** [RunManager] was created and has not started yet. It can now be setup. */
    CREATED,

    /** [RunManager] was started and setup; it is ready to run [Task]s. */
    ACTIVE,

    /** [RunManager] was terminated and cannot run anymore [Task]s. */
    TERMINATED;

    /**
     * Converts this [RunManagerStatus] to the corresponding [ApiEvaluationStatus].
     *
     * @return [ApiEvaluationStatus]
     */
    fun toApi(): ApiEvaluationStatus = when (this) {
        CREATED -> ApiEvaluationStatus.CREATED
        ACTIVE -> ApiEvaluationStatus.ACTIVE
        TERMINATED -> ApiEvaluationStatus.TERMINATED
    }
}