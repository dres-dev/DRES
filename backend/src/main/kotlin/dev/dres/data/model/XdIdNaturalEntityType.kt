package dev.dres.data.model

import kotlinx.dnq.XdNaturalEntityType
import java.util.UUID

open class XdIdNaturalEntityType<T : PersistentEntity> : XdNaturalEntityType<T>() {

    override fun new(init: (T.() -> Unit)): T {
        val transaction = (entityStore.threadSession
            ?: throw IllegalStateException("New entities can be created only in transactional block"))
        return wrap(transaction.newEntity(entityType)).apply {
            constructor()
            this.id = UUID.randomUUID().toString() //default initializer
            init()
        }
    }

}