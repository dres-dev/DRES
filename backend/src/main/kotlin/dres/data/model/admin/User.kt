package dres.data.model.admin

import dres.data.model.Entity

data class User (override var id: Long = -1, val username: String, val password: String, val role: Role) : Entity