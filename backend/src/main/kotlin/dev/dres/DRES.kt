package dev.dres

import dev.dres.api.cli.Cli
import dev.dres.api.cli.OpenApiCommand
import dev.dres.api.rest.RestApi
import dev.dres.data.model.Config
import dev.dres.data.model.admin.Role
import dev.dres.data.model.admin.User
import dev.dres.data.model.audit.AuditLogEntry
import dev.dres.data.model.audit.AuditLogSource
import dev.dres.data.model.audit.AuditLogType
import dev.dres.data.model.template.EvaluationTemplate
import dev.dres.data.model.template.task.*
import dev.dres.data.model.template.task.options.*
import dev.dres.data.model.template.team.Team
import dev.dres.data.model.template.team.TeamAggregator
import dev.dres.data.model.template.team.TeamGroup
import dev.dres.data.model.media.MediaCollection
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.MediaSegment
import dev.dres.data.model.media.MediaType
import dev.dres.data.model.run.Evaluation
import dev.dres.data.model.run.Task
import dev.dres.data.model.submissions.Submission
import dev.dres.data.model.submissions.Verdict
import dev.dres.data.model.submissions.VerdictStatus
import dev.dres.data.model.submissions.VerdictType
import dev.dres.mgmt.admin.UserManager
import dev.dres.run.RunExecutor
import dev.dres.run.audit.AuditLogger
import dev.dres.run.eventstream.EventStreamProcessor
import dev.dres.run.eventstream.handlers.ResultLogStatisticsHandler
import dev.dres.run.eventstream.handlers.SubmissionStatisticsHandler
import dev.dres.run.eventstream.handlers.TeamCombinationScoreHandler
import dev.dres.utilities.FFmpegUtil
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.XdModel
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import java.io.File


object DRES {

    const val VERSION = "2.0.0"

    /** Application root; should pe relative to JAR file or classes path. */
    val rootPath = File(FFmpegUtil::class.java.protectionDomain.codeSource.location.toURI()).toPath()

    init {
        //redirect log of JLine3 from jdk logger to log4j
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val config = if (args.isNotEmpty()) {
            Config.read(File(args[0]))
        } else {
            null
        } ?: Config()

        println("Starting DRES at $rootPath")
        println("Found FFmpeg at ${FFmpegUtil.ffmpegBin}")
        println("Initializing...")

        /* Initialize Xodus based data store. */
        val store = this.prepareDatabase(config)

        /* Initialize UserManager. */
        UserManager.init(store)

        /* Initialize RunExecutor. */
        RunExecutor.init(store)

        /* Initialize AuditLogger */
        AuditLogger.init(store)

        /* Initialize EventStreamProcessor */
        EventStreamProcessor.register(SubmissionStatisticsHandler(), ResultLogStatisticsHandler(store), TeamCombinationScoreHandler())
        EventStreamProcessor.init()

        /* Initialize Rest API. */
        RestApi.init(config, store)

        println("Initialization complete!")

        if (args.isNotEmpty() && args.first() == "openapi") {
            OpenApiCommand().parse(args)
        } else {
            Cli.loop(store, config) //blocks until quit command is given
        }

        /* Stop. */
        RestApi.stop()
        RunExecutor.stop()
        EventStreamProcessor.stop()
        FFmpegUtil.stop()
    }

    /**
     * Loads and prepares the database.
     *
     * @param config The [Config]
     */
    private fun prepareDatabase(config: Config): TransientEntityStore  {
        XdModel.registerNodes(
            AuditLogEntry,
            AuditLogSource,
            AuditLogType,
            ConfiguredOption,
            Evaluation,
            EvaluationTemplate,
            Hint,
            HintOption,
            HintType,
            MediaType,
            MediaCollection,
            MediaItem,
            MediaSegment,
            Role,
            ScoreOption,
            Submission,
            SubmissionOption,
            TargetType,
            TargetOption,
            Task,
            TaskTemplate,
            TaskTemplateTarget,
            TaskOption,
            Team,
            TeamAggregator,
            TeamGroup,
            User,
            Verdict,
            VerdictType,
            VerdictStatus,
        )
        val store = StaticStoreContainer.init(dbFolder = File(config.dataPath), entityStoreName = "dres-db")
        initMetaData(XdModel.hierarchy, store)
        return store
    }
}
