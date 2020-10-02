package dev.dres.utilities.extensions

import dev.dres.api.rest.handler.SessionId
import dev.dres.data.model.UID
import dev.dres.data.model.admin.PlainPassword
import dev.dres.data.model.admin.UserName
import dev.dres.mgmt.admin.UserManager
import java.util.*

fun String?.toUsername(): UserName? {
    if (this == null || this.isEmpty()) {
        return null
    }
    if (this.length < UserManager.MIN_LENGTH_USERNAME) {
        throw IndexOutOfBoundsException("Username too short. Must be at least ${UserManager.MIN_LENGTH_USERNAME}")
    }
    return UserName(this)
}

fun String?.toPlainPassword(): PlainPassword? {
    if (this == null || this.isEmpty()) {
        return null
    }
    if (this.length < UserManager.MIN_LENGTH_PASSWORD) {
        throw IndexOutOfBoundsException("Password too short. Must be at least ${UserManager.MIN_LENGTH_PASSWORD}")
    }
    return PlainPassword(this)
}

fun String.toSessionId(): SessionId {
    return SessionId(this)
}

fun String.UID(): UID = UID(UUID.fromString(this))