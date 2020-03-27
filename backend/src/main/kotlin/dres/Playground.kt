package dres

import dres.data.dbo.DAO
import dres.data.dbo.DataAccessLayer
import dres.data.model.Config
import dres.data.model.admin.PlainPassword
import dres.data.model.admin.Role
import dres.data.model.admin.User
import dres.data.model.admin.UserName
import dres.data.model.basics.*
import dres.data.model.competition.*
import dres.data.serializers.UserSerializer
import org.mindrot.jbcrypt.BCrypt
import java.awt.Color
import java.nio.file.Path
import java.nio.file.Paths

object Playground {

    @JvmStatic
    fun main(args: Array<String>) {

        val config = Config()

        val dataAccessLayer = DataAccessLayer(Paths.get(config.dataPath))

        val collection = dataAccessLayer.collections[1]!!

        val videoItem = dataAccessLayer.mediaItems[2]!! as MediaItem.VideoItem

        val task = Task(-1, "T-KIS-1", TaskType.KIS_VISUAL, false,
                TaskDescription.KisTextualTaskDescription(
                        videoItem, TemporalRange(
                        TemporalPoint(1.0, TemporalUnit.SECONDS), TemporalPoint(3.0, TemporalUnit.SECONDS)
                ), listOf<String>("a segment", "a segment with a description")
                ))

        val team = Team(-1, "testTeam", 1, "#FFFF00", "testTeamLogo.png") //TODO teams should probably be stored in runs rather than competitions

        val competition = Competition(-1, "testCompetition", "just a test", listOf(task), listOf(team))

        dataAccessLayer.competitions.append(competition)

    }

}