package dev.dres

import dev.dres.api.cli.Cli
import dev.dres.api.cli.OpenApiCommand
import dev.dres.api.rest.RestApi
import dev.dres.data.model.Config
import dev.dres.data.model.audit.AuditLogEntry
import dev.dres.data.model.audit.AuditLogSource
import dev.dres.data.model.audit.AuditLogType
import dev.dres.data.model.basics.media.MediaCollection
import dev.dres.data.model.basics.media.MediaItem
import dev.dres.data.model.basics.media.MediaItemSegment
import dev.dres.data.model.basics.media.MediaType
import dev.dres.mgmt.admin.UserManager
import dev.dres.run.RunExecutor
import dev.dres.run.audit.AuditLogger
import dev.dres.run.eventstream.EventStreamProcessor
import dev.dres.run.eventstream.handlers.ResultLogStatisticsHandler
import dev.dres.run.eventstream.handlers.SubmissionStatisticsHandler
import dev.dres.run.eventstream.handlers.TeamCombinationScoreHandler
import dev.dres.utilities.FFmpegUtil
import kotlinx.dnq.XdModel
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import java.io.File


object DRES {

    const val VERSION = "1.2.2"

    /** Application root; shoud pe relative to JAR file or classes path. */
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
        XdModel.registerNodes(
            AuditLogSource,
            AuditLogType,
            AuditLogEntry,
            MediaType,
            MediaCollection,
            MediaItem,
            MediaItemSegment
        )
        val store = StaticStoreContainer.init(dbFolder = File(config.dataPath), entityStoreName = "dres-db")
        initMetaData(XdModel.hierarchy, store)

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
}
