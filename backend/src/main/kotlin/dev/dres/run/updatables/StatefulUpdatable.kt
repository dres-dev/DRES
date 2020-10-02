package dev.dres.run.updatables

/**
 * An [Updatable] that has an internal state regarding its dirtyness.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
interface StatefulUpdatable : Updatable {
    /** Flag indicating whether this [StatefulUpdatable] requires an update or not */
    var dirty: Boolean
}