package dev.dres.data.model.admin

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import dev.dres.data.model.Entity
import dev.dres.data.model.UID
import org.mindrot.jbcrypt.BCrypt

typealias UserId = UID

data class User constructor(
        override var id: UserId = UID.EMPTY,
        val username: UserName,
        val password: HashedPassword,
        val role: Role) : Entity {
    override fun toString(): String = "User(id=$id, username=${username.name}, role=$role)"
}

data class  UserName @JsonCreator constructor(@JsonProperty("name") val name: String){
    val length = name.length
}

sealed class Password(private val pass: String)

class PlainPassword(internal val pass: String) : Password(pass){

    fun hash(): HashedPassword {
        return HashedPassword(BCrypt.hashpw(pass, BCrypt.gensalt()))
    }

    val length = pass.length

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlainPassword

        if (pass != other.pass) return false

        return true
    }

    override fun hashCode(): Int {
        return pass.hashCode()
    }


}

class HashedPassword @JsonCreator constructor (@JsonProperty("hash") val hash: String) : Password(hash) {
    fun check(plain: PlainPassword): Boolean {
        return BCrypt.checkpw(plain.pass, this.hash)
    }
}