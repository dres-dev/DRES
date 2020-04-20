package dres.data.dbo

import dres.data.model.Entity

/**
 * Wrapper for DAO which enables index-based access
 */
class DaoIndexer<T: Entity, K>(private val dao: DAO<T>, private val keyTransform: (T) -> K) {

    private val index: MutableMap<K, MutableList<Long>> = mutableMapOf()

    init {
        //load DAO to index
        update()
    }

    /**
     * rebuilds the index
     */
    fun update() {
        dao.forEach {

            val key = keyTransform(it)

            if (!index.containsKey(key)){
                index[key] = mutableListOf(it.id)
            } else {
                index[key]!!.add(it.id)
            }

        }
    }

    operator fun get(key: K): List<T>{
        val keys = index[key] ?: return emptyList()
        return keys.mapNotNull { dao[it] }
    }

}