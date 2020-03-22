package dres.data.model.admin

import dres.data.model.Entity
import org.mindrot.jbcrypt.BCrypt

data class User (override var id: Long = -1, val username: UserName, val password: HashedPassword, val role: Role) : Entity

data class UserName(val name: String)

sealed class Password(private val pass: String)

class PlainPassword(internal val pass: String) : Password(pass){

    fun hash(): HashedPassword {
        return HashedPassword(BCrypt.hashpw(pass, BCrypt.gensalt()))
    }

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

class HashedPassword(private val passHash: String) : Password(passHash){

    val hash
        get() = passHash

    fun check(plain: PlainPassword): Boolean {
        return BCrypt.checkpw(plain.pass, passHash)
    }

}