package dres.data.model

import java.util.*


inline class UID (val string: String) {

    companion object {
        val EMPTY = UID("00000000-0000-000000000-000000000000")
    }

    constructor() : this(UUID.randomUUID().toString())
    constructor(uuid: UUID) : this(uuid.toString())

    override fun toString(): String {
        return "UID($string)"
    }


}

