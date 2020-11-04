package dev.dres.data.dbo

import dev.dres.data.model.Entity
import dev.dres.data.model.UID
import dev.dres.utilities.extensions.optimisticRead
import dev.dres.utilities.extensions.write
import java.util.concurrent.locks.StampedLock

/**
 * Wrapper for DAO which enables index-based access
 */
open class DaoIndexer<T: Entity, K> internal constructor(internal val dao: DAO<T>, internal val keyTransform: (T) -> K) {

    internal val index: MutableMap<K, MutableList<UID>> = mutableMapOf()

    internal val lock = StampedLock()

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

    fun find(predicate: (K) -> Boolean): List<T> = this.lock.optimisticRead {
        val key = this.index.keys.find(predicate)
        return if (key == null) emptyList() else this[key]
    }

    fun filter(predicate: (K) -> Boolean): List<T> = this.lock.optimisticRead {
        return this.index.keys.filter(predicate).flatMap { this[it] }
    }

}