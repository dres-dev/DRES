package dres.run.updatables

import dres.api.rest.types.run.RunState
import dres.run.RunManagerStatus

/**
 * Interface implemented by classes that are updated during the lifecycle of a [RunManager].
 *
 * @author Ralph Gasser
 * @version 1.0
 */
interface Updatable {
    /**
     * Triggers an update of this [Updatable].
     *
     * @param state The [RunManagerStatus] for which to provide an update.
     */
    fun update(status: RunManagerStatus)

    /**
     * Returns true, if this [Updatable] should be updated given the [RunState] and false otherwise.
     *
     * @param status The [RunManagerStatus] to check
     * @return True if update is required, false otherwise.
     */
    fun shouldBeUpdated(status: RunManagerStatus): Boolean
}