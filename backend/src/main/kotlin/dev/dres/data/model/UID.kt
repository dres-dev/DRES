package dev.dres.data.model

import java.util.*


inline class UID (val string: String) {
    companion object {
        val EMPTY = UID(UUID(0L, 0L))
    }
    constructor() : this(UUID.randomUUID().toString())
    constructor(uuid: UUID) : this(uuid.toString())
    override fun toString(): String = "UID($string)"
}

