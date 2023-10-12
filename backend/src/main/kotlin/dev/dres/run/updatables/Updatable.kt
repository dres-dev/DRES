package dev.dres.run.updatables

import dev.dres.api.rest.types.evaluation.ApiEvaluationState
import dev.dres.api.rest.types.evaluation.ApiTaskStatus
import dev.dres.data.model.run.RunActionContext
import dev.dres.run.RunManagerStatus

/**
 * Interface implemented by classes that are updated during the lifecycle of a [RunManager].
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
interface Updatable {

    /** The [Phase] this [Updatable] belongs to. */
    val phase: Phase

    /**
     * Triggers an update of this [Updatable].
     *
     * @param runStatus The [RunManagerStatus] to check.
     * @param taskStatus The [ApiTaskStatus] to check. Can be null
     * @param context The [RunActionContext] used to invoke this [Updatable].
     */
    fun update(runStatus: RunManagerStatus, taskStatus: ApiTaskStatus? = null, context: RunActionContext = RunActionContext.INTERNAL)

    /**
     * Returns true, if this [Updatable] should be updated given the [ApiEvaluationState] and false otherwise.
     *
     * @param runStatus The [RunManagerStatus] to check.
     * @param taskStatus The [ApiTaskStatus] to check. Can be null
     * @return True if update is required, false otherwise.
     */
    fun shouldBeUpdated(runStatus: RunManagerStatus, taskStatus: ApiTaskStatus? = null): Boolean
}