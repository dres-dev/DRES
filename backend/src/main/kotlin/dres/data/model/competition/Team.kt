package dres.data.model.competition

import dres.data.model.Entity
import java.awt.Color
import java.nio.file.Path

data class Team (override var id: Long, val name: String, val number: Int, val color: String, val logoLocation: String) : Entity {
    fun logo(): Path = Path.of(logoLocation)
}