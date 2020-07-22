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
//import dres.run.filter.DuplicateSubmissionFilter
//import dres.run.filter.OneCorrectSubmissionPerTeamFilter
//import dres.run.filter.SubmissionFilter
//import dres.run.filter.TemporalSubmissionFilter
//import dres.run.score.interfaces.TaskRunScorer
//import dres.run.score.scorer.KisTaskScorer
//import dres.run.validation.TemporalOverlapSubmissionValidator
//import java.io.File
//import java.io.FileInputStream
//import java.io.PrintStream
//import java.util.*
//
///**
// * Describes a visual Known Item Search (KIS)
// *
// * @param item [MediaItem] the user should be looking for.
// */
//data class KisVisualTaskDescription @JsonCreator constructor(
//        @JsonProperty("uid") override val uid: String = UUID.randomUUID().toString(),
//        @JsonProperty("name") override val name: String,
//        @JsonProperty("taskGroup") override val taskGroup: TaskGroup,
//        @JsonProperty("duration") override val duration: Long,
//        @JsonProperty("item") override val item: MediaItem.VideoItem,
//        @JsonProperty("temporalRange") override val temporalRange: TemporalRange)
//    : TaskDescription, MediaSegmentTaskDescription {
//    override fun toQueryDescription(config: Config): QueryDescription {
//
//        val file = File( File(config.cachePath + "/tasks"), cacheItemName())
//
//        return FileInputStream(file).use { imageInFile ->
//            val fileData = ByteArray(file.length().toInt())
//            imageInFile.read(fileData)
//            QueryDescription(name, QueryContent(video = listOf(QueryContentElement(Base64.getEncoder().encodeToString(fileData), "video/mp4"))))
//        }
//    }
//
//    override fun printOverview(out: PrintStream) {
//        out.println("Visual Known Item Search Task '${name}' (${taskGroup.name}, ${taskGroup.type})")
//        out.println("Target: ${item.name} ${temporalRange.start} to ${temporalRange.end}")
//        out.println("Task Duration: ${duration}")
//    }
//
//    override fun textualDescription(): String? = null
//
//    override val defaultMediaCollectionId: Long
//        get() = item.collection
//
//    //override fun newScorer(): TaskRunScorer = KisTaskScorer()
//    override fun newValidator() = TemporalOverlapSubmissionValidator(this)
//    override fun cacheItemName() = "${taskGroup.name}-${item.collection}-${item.id}-${temporalRange.start.value}-${temporalRange.end.value}.mp4"
//    //override fun newFilter(): SubmissionFilter = TemporalSubmissionFilter() and OneCorrectSubmissionPerTeamFilter() and DuplicateSubmissionFilter()
//}