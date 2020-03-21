package dres.data.model.competition

import java.awt.Color
import java.nio.file.Path

data class Team (val id: Long, val name: String, val number: Int, val color: Color, val logo: Path)