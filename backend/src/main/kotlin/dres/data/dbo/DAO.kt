package dres.data.dbo

import dres.data.model.Entity
import dres.utilities.extensions.optimisticRead
import dres.utilities.extensions.write
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.Serializer
import java.nio.file.Path
import java.util.concurrent.locks.StampedLock

/**
 * A simple data access object [DAO] implementation for the [Entity] objects used by DRES.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class DAO<T: Entity>(path: Path, private val serializer: Serializer<T>) : Iterable<T>, AutoCloseable {

    init {
        path.parent.toFile().mkdirs()
    }

    /** The [DB] object used to store */
    private val db = DBMaker.fileDB(path.toFile()).transactionEnable().fileMmapEnableIfSupported().make()

    /** Internal counter used to keep track of the next autoincrement ID.  */
    private val autoincrement = this.db.atomicLong("counter", 0L).createOrOpen()

    /** Internal data structure used to keep track of the data held by this [DAO]. */
    private val data = this.db.hashMap("data", Serializer.LONG, this.serializer).counterEnable().createOrOpen()

    /** Stamped lock to mediate read/write operations through this [DAO]. */
    private val lock: StampedLock = StampedLock()

    private val indexers = mutableListOf<DaoIndexer<T, *>>()

    /** Name of the entity accessed through this [DAO]. */
    val name = path.fileName.toString().replace(".db","")

    init {
        this.db.commit()
    }

    /**
     * Returns the value [T] for the given ID.
     *
     * @param id The ID of the entry.
     * @return Entry [T]
     */
    operator fun get(id: Long) = this.lock.optimisticRead { this.data[id] }

    /**
     * Returns true if value for given key exists and false otherwise.
     *
     * @param id The key of the entry.
     * @return Entry [T]
     */
    fun exists(id: Long) = this.lock.optimisticRead { this.data.containsKey(id) }

    /**
     * Deletes the value [T] for the given ID.
     *
     * @param id The ID of the entry that should be deleted
     * @return Deleted entry [T]
     */
    fun delete(id: Long): T? = this.lock.write {
        val deleted = this.data.remove(id)
        this.db.commit()
        if(deleted != null){
            indexers.forEach {
                it.delete(deleted)
            }
        }
        return deleted
    }

    /**
     * Deletes the value [T]
     *
     * @param value The value that should be deleted.
     * @return Deleted entry [T]
     */
    fun delete(value: T) = this.delete(value.id)


    /**
     * Deletes all values with given ids
     */
    fun batchDelete(ids: Iterable<Long>) = this.lock.write {
        for (id in ids){
            val t = data[id]
            if(t != null){
                indexers.forEach {
                    it.delete(t)
                }
            }
            this.data.remove(id)
        }
        this.db.commit()
    }

    /**
     * Updates the value for the given ID with the new value [T]
     *
     * @param id The ID of the value that should be updated
     * @param value The new value [T]
     */
    fun update(id: Long, value: T) = this.lock.write {
        if (id <= this.autoincrement.get()) {
            val old = this.data[id]
            this.data[id] = value
            indexers.forEach {
                if (old != null){
                    it.delete(old)
                }
                it.append(value)
            }
            this.db.commit()
        } else {
            throw IndexOutOfBoundsException("Could not update value with ID $id because such a value doesn't exist.")
        }
    }

    /**
     * Updates the value [T]
     *
     * @param value The new value [T]
     */
    fun update(value: T) = this.update(value.id, value)

    /**
     * Appends the given value using this [DAO]
     *
     * @param value The value [T] that should be appended
     * @return ID of the new value.
     */
    fun append(value: T): Long = this.lock.write {
        val next = this.autoincrement.incrementAndGet()
        value.id = next
        this.data[next] = value
        this.db.commit()
        indexers.forEach {
            it.append(value)
        }
        return next
    }

    /**
     * Appends the given values using this [DAO]
     *
     * @param values An iterable of the values [T] that should be appended.
     */
    fun batchAppend(values: Iterable<T>) = this.lock.write {
        for (value in values) {
            val next = this.autoincrement.incrementAndGet()
            value.id = next
            this.data[next] = value
            indexers.forEach {
                it.append(value)
            }
        }
        this.db.commit()
        this.data.values
    }

    /**
     * Closes this [DAO]
     */
    override fun close() {
        if (!this.db.isClosed()) {
            this.data.close()
            this.db.close()
        }
    }

    /**
     * Returns an iterator over the elements of this object.
     */
    override fun iterator(): Iterator<T> = object : Iterator<T> {
        private var id: Long = 1L

        override fun hasNext(): Boolean = this@DAO.lock.optimisticRead {
            for (id in this.id..this@DAO.autoincrement.get()) {
                if (this@DAO.data.containsKey(id)) {
                    this.id = id
                    return true
                }
            }
            return false
        }

        override fun next(): T = this@DAO.lock.optimisticRead {
            if (this@DAO.data.containsKey(this.id)) {
                return this@DAO.data[this.id++]!!
            }
            throw NoSuchElementException("There is no element with ID ${this.id} for DAO '${this@DAO.name}'.")
        }
    }

    fun filter(predicate: (T) -> Boolean): List<T> = this.lock.optimisticRead {
        return this.data.values.filterNotNull().filter(predicate)
    }

    fun <R> map(transform: (T) -> R): List<R> = this.lock.optimisticRead {
        return this.data.values.filterNotNull().map(transform)
    }

    fun find(predicate: (T) -> Boolean): T? = this.lock.optimisticRead {
        return this.data.values.find{ it != null && predicate(it) }
    }

    fun forEach(action: (T) -> Unit): Unit = this.lock.optimisticRead {
        return this.data.values.filterNotNull().forEach(action)
    }

    fun <K> groupBy(keySelector: (T) -> K): Map<K, List<T>> = this.lock.optimisticRead {
        return this.data.values.filterNotNull().groupBy(keySelector)
    }

    internal fun <K> addIndexer(daoIndexer: DaoIndexer<T, K>) {
        indexers.add(daoIndexer)
    }
}