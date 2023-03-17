package dev.dres.api.rest.types.evaluation

import dev.dres.data.model.run.DbEvaluationStatus
import dev.dres.data.model.run.DbEvaluationType
import dev.dres.run.RunManager
import dev.dres.run.RunManagerStatus

/**
 * The collection auf statuses an [ApiEvaluation] and the associated [RunManager] can be in.
 *
 * API version of [DbEvaluationStatus]
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

    /**
     * Converts this [ApiEvaluationStatus] to a [DbEvaluationStatus] representation. Requires an ongoing transaction.
     *
     * @return [DbEvaluationStatus]
     */
    fun toDb(): DbEvaluationStatus = when(this) {
        CREATED -> DbEvaluationStatus.CREATED
        ACTIVE -> DbEvaluationStatus.ACTIVE
        TERMINATED -> DbEvaluationStatus.TERMINATED
    }
}