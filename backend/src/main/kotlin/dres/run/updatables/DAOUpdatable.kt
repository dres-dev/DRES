package dres.run.updatables

import dres.data.dbo.DAO
import dres.data.model.Entity
import dres.run.RunManagerStatus

/**
 * A [StatefulUpdatable] that takes care of storing the object it holds whenever [DAOUpdatable.update]
 * is called and changes have been registered.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class DAOUpdatable<T: Entity>(val dao: DAO<T>, val obj: T): StatefulUpdatable {

    @Volatile
    override var dirty: Boolean = false

    override fun update(status: RunManagerStatus) {
        if (this.dirty) {
            this.dao.update(this.obj)
        }
    }

    override fun shouldBeUpdated(status: RunManagerStatus): Boolean = true
}