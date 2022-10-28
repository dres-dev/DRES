package dev.dres

import dev.dres.api.cli.Cli
import dev.dres.api.cli.OpenApiCommand
import dev.dres.api.rest.RestApi
import dev.dres.data.model.Config
import dev.dres.data.model.audit.AuditLogEntry
import dev.dres.data.model.audit.AuditLogSource
import dev.dres.data.model.audit.AuditLogType
import dev.dres.data.model.competition.CompetitionDescription
import dev.dres.data.model.competition.task.*
import dev.dres.data.model.competition.task.options.*
import dev.dres.data.model.competition.team.Team
import dev.dres.data.model.competition.team.TeamAggregator
import dev.dres.data.model.competition.team.TeamGroup
import dev.dres.data.model.media.MediaCollection
import dev.dres.data.model.media.MediaItem
import dev.dres.data.model.media.MediaItemSegment
import dev.dres.data.model.media.MediaType
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
            AuditLogSource,
            AuditLogType,
            AuditLogEntry,
            MediaType,
            MediaCollection,
            MediaItem,
            MediaItemSegment,
            ConfiguredOption,
            TaskComponentOption,
            TaskOption,
            TaskScoreOption,
            TaskSubmissionOption,
            TaskTargetOption,
            CompetitionDescription,
            TaskDescription,
            Team,
            TeamGroup,
            TeamAggregator,
            HintType,
            Hint,
            TargetType,
            TaskDescriptionTarget
        )
        val store = StaticStoreContainer.init(dbFolder = File(config.dataPath), entityStoreName = "dres-db")
        initMetaData(XdModel.hierarchy, store)
        return store
    }
}
