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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.mindrot.jbcrypt.BCrypt
import java.awt.Color
import java.nio.file.Path
import java.nio.file.Paths

object Playground {

    @JvmStatic
    fun main(args: Array<String>) {

//        val config = Config()
//
//        val dataAccessLayer = DataAccessLayer(Paths.get(config.dataPath))
//
//        val collection = dataAccessLayer.collections[1]!!
//
//        val videoItem = dataAccessLayer.mediaItems[2]!! as MediaItem.VideoItem
//
//        val task = Task(-1, "T-KIS-1", TaskType.KIS_VISUAL, "Textual Expert KIS",
//                TaskDescription.KisTextualTaskDescription(
//                        videoItem, TemporalRange(
//                        TemporalPoint(1.0, TemporalUnit.SECONDS), TemporalPoint(3.0, TemporalUnit.SECONDS)
//                ), listOf<String>("a segment", "a segment with a description")
//                ))
//
//        val team = Team(-1, "testTeam", 1, "#FFFF00", "testTeamLogo.png") //TODO teams should probably be stored in runs rather than competitions
//
//        val competition = Competition(-1, "testCompetition", "just a test", mutableListOf(task), mutableListOf(team))
//
//        dataAccessLayer.competitions.append(competition)

        val inputString = """
             {
    "name": "T-KIS-1",
    "type": "KIS_VISUAL",
    "taskGroup": "Textual Expert KIS",
    "description": {
      "item": {
        "id": 2,
        "name": "testVideo",
        "location": "v3c1\\testVideo.mp4",
        "collection": 1,
        "ms": 10000000,
        "fps": 24.0
      },
      "temporalRange": {
        "start": {
          "value": 1.0,
          "unit": "SECONDS"
        },
        "end": {
          "value": 3.0,
          "unit": "SECONDS"
        }
      },
      "descriptions": [
        "a segment",
        "a segment with a description"
      ],
      "delay": 30
    }
  }
        """.trimIndent()

        val json = Json(JsonConfiguration.Stable)

        val videoItem = MediaItem.VideoItem(5, "testVideo", "v3c1\\testvideo.mp4", 1, 1000000, 24.0f)


        val description = TaskDescription.KisTextualTaskDescription(
                videoItem, TemporalRange(
                TemporalPoint(1.0, TemporalUnit.SECONDS), TemporalPoint(3.0, TemporalUnit.SECONDS)
        ), listOf<String>("a segment", "a segment with a description")
        )

        val descriptionString = json.stringify(TaskDescription.serializer(), description)

        println(description)
        println(descriptionString)

        val description2 = json.parse(TaskDescription.serializer(), descriptionString)

        println(description2)

//
//        val task = Task(-1, "T-KIS-1", TaskType.KIS_VISUAL, "Textual Expert KIS",
//                TaskDescription.KisTextualTaskDescription(
//                        videoItem, TemporalRange(
//                        TemporalPoint(1.0, TemporalUnit.SECONDS), TemporalPoint(3.0, TemporalUnit.SECONDS)
//                ), listOf<String>("a segment", "a segment with a description")
//                ))
//
//
//        val taskString = json.stringify(Task.serializer(), task)
//
//        println(task)
//        println(taskString)
//        println(json.parse(Task.serializer(), taskString))


    }

}