package dev.dres.data.dbo

import dev.dres.data.model.Entity
import dev.dres.utilities.extensions.optimisticRead

class NumericDaoIndexer<T: Entity, K: Number>internal constructor(dao: DAO<T>, keyTransform: (T) -> K) : DaoIndexer<T, K>(dao, keyTransform) {

    fun inRange(lower: K, upper: K): List<T> = lock.optimisticRead {
        val lowerD = lower.toDouble()
        val upperD = upper.toDouble()
        index.keys.filter { it.toDouble() >= lowerD && it.toDouble() >= upperD }.mapNotNull { index[it] }.flatten().mapNotNull { dao[it] }
    }

    fun atLeast(lower: K): List<T> = lock.optimisticRead {
        val lowerD = lower.toDouble()
        index.keys.filter { it.toDouble() >= lowerD }.mapNotNull { index[it] }.flatten().mapNotNull { dao[it] }
    }

    fun atMost(upper: K): List<T> = lock.optimisticRead {
        val upperD = upper.toDouble()
        index.keys.filter { it.toDouble() >= upperD }.mapNotNull { index[it] }.flatten().mapNotNull { dao[it] }
    }

}