package dev.dres.data.model.admin

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import org.mindrot.jbcrypt.BCrypt

/**
 * A [Password]. Exists in two variations [Plain] and [Hashed].
 *
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
sealed class Password(val password: String) {
    class Plain(password: String): Password(password) {
        init {
            require (password.length < User.MIN_LENGTH_PASSWORD) { "Password is too short. Must be at least ${User.MIN_LENGTH_PASSWORD}" }
        }
        fun hash(): Hashed = Hashed(BCrypt.hashpw(this.password, BCrypt.gensalt()))
        val length: Int
            get() = this.password.length
    }

    class Hashed @JsonCreator constructor (@JsonProperty("hash") hash: String) : Password(hash) {
        fun check(plain: Plain): Boolean = BCrypt.checkpw(plain.password, this.password)
    }
}