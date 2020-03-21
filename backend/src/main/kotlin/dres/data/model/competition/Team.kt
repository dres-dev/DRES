package dres.data.model.competition

import java.awt.Color
import java.nio.file.Path

data class Team (val id: String, val name: String, val number: Number, val color: Color, val logo: Path)