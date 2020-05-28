package dres.utilities.extensions

import dres.api.rest.handler.UserHandler
import dres.data.model.admin.PlainPassword
import dres.data.model.admin.UserName
import dres.mgmt.admin.UserManager

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

fun String.toSessionId(): UserHandler.SessionId {
    return UserHandler.SessionId(this)
}