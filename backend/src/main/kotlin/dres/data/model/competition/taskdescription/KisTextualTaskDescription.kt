package dres.data.model.competition.taskdescription
//
//import com.fasterxml.jackson.annotation.JsonCreator
//import com.fasterxml.jackson.annotation.JsonProperty
//import dres.data.model.Config
//import dres.data.model.basics.media.MediaItem
//import dres.data.model.basics.time.TemporalRange
//import dres.data.model.competition.QueryContent
//import dres.data.model.competition.QueryContentElement
//import dres.data.model.competition.QueryDescription
//import dres.data.model.competition.TaskGroup
//import dres.data.model.competition.interfaces.MediaSegmentTaskDescription
//import dres.data.model.competition.interfaces.TaskDescription
//
//import dres.run.validation.TemporalOverlapSubmissionValidator
//import java.io.File
//import java.io.FileInputStream
//import java.io.PrintStream
//import java.util.*
//
///**
// * Describes a textual Known Item Search (KIS) [Task]
// *
// * @param item [MediaItem] the user should be looking for.
// */
//data class KisTextualTaskDescription @JsonCreator constructor(
//        @JsonProperty("uid") override val uid: String = UUID.randomUUID().toString(),
//        @JsonProperty("name") override val name: String,
//        @JsonProperty("taskGroup") override val taskGroup: TaskGroup,
//        @JsonProperty("duration") override val duration: Long,
//        @JsonProperty("item") override val item: MediaItem.VideoItem,
//        @JsonProperty("temporalRange") override val temporalRange: TemporalRange,
//        @JsonProperty("descriptions") val descriptions: List<String>,
//        @JsonProperty("delay") val delay: Int = 30)
//    : TaskDescription, MediaSegmentTaskDescription {
//
//    override fun toQueryDescription(config: Config): QueryDescription {
//
//        val file = File(File(config.cachePath + "/tasks"), cacheItemName())
//
//        return FileInputStream(file).use { imageInFile ->
//            val fileData = ByteArray(file.length().toInt())
//            imageInFile.read(fileData)
//            QueryDescription(name,
//                    query = QueryContent(text = descriptions.mapIndexed { i, s -> QueryContentElement(s, "text/plain", i * delay) } ),
//                    reveal = QueryContent(video = listOf(QueryContentElement(Base64.getEncoder().encodeToString(fileData), "video/mp4")))
//            )
//        }
//    }
//
//
//
//    override fun printOverview(out: PrintStream) {
//        println("Textual Known Item Search Task '${name}' (${taskGroup.name}, ${taskGroup.type})")
//        println("Target: ${item.name} ${temporalRange.start} to ${temporalRange.end}")
//        println("Task Duration: ${duration}")
//        println("Query Text:")
//        descriptions.forEach(::println)
//    }
//
//    override fun textualDescription(): String = if (descriptions.isNotEmpty()) descriptions.last() else name
//
//    override val defaultMediaCollectionId: Long
//        get() = item.collection
//
//    //override fun newScorer(): TaskRunScorer = KisTaskScorer()
//    override fun newValidator() = TemporalOverlapSubmissionValidator(this)
//    override fun cacheItemName() = "${taskGroup.name}-${item.collection}-${item.id}-${temporalRange.start.value}-${temporalRange.end.value}.mp4"
//    //override fun newFilter(): SubmissionFilter = TemporalSubmissionFilter() and OneCorrectSubmissionPerTeamFilter() and DuplicateSubmissionFilter()
//}