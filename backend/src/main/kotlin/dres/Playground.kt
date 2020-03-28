package dres

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import dres.data.model.competition.Task
import dres.data.model.competition.TaskDescription


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
    "id": -1,
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
        "fps": 24.0,
        "itemType": "video"
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
      "delay": 30,
      "taskType": "KIS_TEXTUAL"
    }
  }"""


        val descriptionString = """{
      "item": {
        "id": 2,
        "name": "testVideo",
        "location": "v3c1\\testVideo.mp4",
        "collection": 1,
        "ms": 10000000,
        "fps": 24.0,
        "itemType": "video"
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
      "delay": 30,
      "taskType": "KIS_TEXTUAL"
    }"""

        val mapper = ObjectMapper().registerModule(KotlinModule())

        val task = mapper.readValue(inputString, Task::class.java)

        println(task)


    }

}