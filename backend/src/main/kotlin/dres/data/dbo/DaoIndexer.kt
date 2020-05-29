package dres.data.dbo

import dres.data.model.Entity
import dres.utilities.extensions.optimisticRead
import dres.utilities.extensions.write
import java.util.concurrent.locks.StampedLock

/**
 * Wrapper for DAO which enables index-based access
 */
class DaoIndexer<T: Entity, K>(private val dao: DAO<T>, private val keyTransform: (T) -> K) {

    private val index: MutableMap<K, MutableList<Long>> = mutableMapOf()

    private val lock = StampedLock()

    init {
        //load DAO to index
        rebuild()
        dao.addIndexer(this)
    }

    /**
     * rebuilds the index
     */
    fun rebuild() = lock.write {
        val map = dao.groupBy( keyTransform ).mapValues { it.value.map { e -> e.id }.toMutableList() }
        index.clear()
        index.putAll(map)
    }

    operator fun get(key: K): List<T> = lock.optimisticRead {
        index[key]?.mapNotNull { dao[it] } ?: emptyList()
    }

    fun keys(): Set<K> = lock.optimisticRead { index.keys }


    internal fun delete(value: T) = lock.write {
        index[keyTransform(value)]?.remove(value.id)
    }

    internal fun append(value: T) = lock.write {
        val key = keyTransform(value)
        if (!index.containsKey(key)) {
            index[key] = mutableListOf(value.id)
        } else {
            index[key]!!.add(value.id)
        }
    }

}