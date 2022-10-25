package dev.dres.run.updatables

import dev.dres.data.dbo.DAO
import dev.dres.data.model.PersistentEntity
import dev.dres.run.RunManagerStatus

/**
 * A [StatefulUpdatable] that takes care of storing the object it holds whenever [DAOUpdatable.update]
 * is called and changes have been registered.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class DAOUpdatable<T: PersistentEntity>(val dao: DAO<T>, val obj: T): StatefulUpdatable {


    companion object {
        val ELIGIBLE_RUNNING_STATES = arrayOf(
            RunManagerStatus.CREATED,
            RunManagerStatus.ACTIVE,
            //RunManagerStatus.TASK_ENDED,
            RunManagerStatus.TERMINATED
        )
    }

    /** The [Phase] this [DAOUpdatable] belongs to. */
    override val phase: Phase = Phase.FINALIZE

    @Volatile
    override var dirty: Boolean = false
    override fun update(status: RunManagerStatus) {
        if (this.dirty) {
            this.dao.update(this.obj)
            this.dirty = false
        }
    }

    /**
     * Checks if [RunManagerStatus] is contained in [ELIGIBLE_RUNNING_STATES]. This should prevent
     * [DAOUpdatable] to be invoked while a task run is running, which potentially involves a lot
     * of changes to the object and therefore a lot of unnecessar updates.
     *
     * @param status The [RunManagerStatus] to check.
     * @return True if [DAOUpdatable] should be run, false otherwise.
     */
    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = (status in ELIGIBLE_RUNNING_STATES)
}