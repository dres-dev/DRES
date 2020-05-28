package dres.data.dbo

import dres.data.model.Entity
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Wrapper for DAO which enables index-based access
 */
class DaoIndexer<T: Entity, K>(private val dao: DAO<T>, private val keyTransform: (T) -> K) {

    private val index: MutableMap<K, MutableList<Long>> = mutableMapOf()

    private val updateLock = ReentrantReadWriteLock()

    init {
        //load DAO to index
        rebuild()
    }

    /**
     * rebuilds the index
     */
    fun rebuild() = updateLock.write {
        val map = dao.groupBy( keyTransform ).mapValues { it.value.map { e -> e.id }.toMutableList() }
        index.clear()
        index.putAll(map)
    }

    operator fun get(key: K): List<T> = updateLock.read {
        index[key]?.mapNotNull { dao[it] } ?: emptyList()
    }

    fun keys(): Set<K> = updateLock.read { index.keys }

}