package dev.dres.data.model.admin

import dev.dres.api.rest.RestApiRole

/**
 * Roles currently supported by DRES.
 */
enum class Role {
    ADMIN, JUDGE, VIEWER, PARTICIPANT;

    companion object {
        fun fromRestRole(role: RestApiRole): Role = when(role) {
            RestApiRole.ANYONE,
            RestApiRole.VIEWER -> VIEWER
            RestApiRole.PARTICIPANT -> PARTICIPANT
            RestApiRole.JUDGE -> JUDGE
            RestApiRole.ADMIN -> ADMIN
        }
    }
}