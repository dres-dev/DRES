package dev.dres.run.updatables

import dev.dres.data.dbo.DAO
import dev.dres.data.model.Entity
import dev.dres.run.RunManagerStatus

/**
 * A [StatefulUpdatable] that takes care of storing the object it holds whenever [DAOUpdatable.update]
 * is called and changes have been registered.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class DAOUpdatable<T: Entity>(val dao: DAO<T>, val obj: T): StatefulUpdatable {

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

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = true
}