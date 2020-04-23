package dres.data.dbo

import dres.data.model.Entity

/**
 * Wrapper for DAO which enables index-based access
 */
class DaoIndexer<T: Entity, K>(private val dao: DAO<T>, private val keyTransform: (T) -> K) {

    private val index: MutableMap<K, MutableList<Long>> = mutableMapOf()

    init {
        //load DAO to index
        rebuild()
    }

    /**
     * rebuilds the index
     */
    fun rebuild() {
        val map = dao.groupBy ( keyTransform ).mapValues { it.value.map { e -> e.id }.toMutableList() }
        index.clear()
        index.putAll(map)

    }

    operator fun get(key: K): List<T>{
        val keys = index[key] ?: return emptyList()
        return keys.mapNotNull { dao[it] }
    }

    fun keys(): Set<K> = index.keys

}