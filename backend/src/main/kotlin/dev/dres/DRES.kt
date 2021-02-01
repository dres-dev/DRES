package dev.dres

import dev.dres.api.cli.Cli
import dev.dres.api.cli.OpenApiCommand
import dev.dres.api.rest.RestApi
import dev.dres.data.dbo.DataAccessLayer
import dev.dres.data.model.Config
import dev.dres.mgmt.admin.UserManager
import dev.dres.run.RunExecutor
import dev.dres.run.audit.AuditLogger
import dev.dres.run.eventstream.EventStreamProcessor
import dev.dres.run.eventstream.handlers.ResultLogStatisticsHandler
import dev.dres.run.eventstream.handlers.SubmissionStatisticsHandler
import dev.dres.run.eventstream.handlers.TeamCombinationScoreHandler
import dev.dres.utilities.FFmpegUtil
import java.io.File
import java.nio.file.Paths

object DRES {

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

        println("initializing...")

        /* Initialize data access layer. */
        val dataAccessLayer = DataAccessLayer(Paths.get(config.dataPath))

        /* Initialize user manager. */
        UserManager.init(dataAccessLayer.users)

        /* Initialize run executor. */
        RunExecutor.init(dataAccessLayer.runs)

        /* Initialize audit logger */
        AuditLogger.init(dataAccessLayer.audit)

        /* Initialize Event Stream Processor */
        EventStreamProcessor.register(SubmissionStatisticsHandler(), ResultLogStatisticsHandler(dataAccessLayer.mediaSegmentItemIdIndex), TeamCombinationScoreHandler())
        EventStreamProcessor.init()

        /* Initialize Rest API. */
        RestApi.init(config, dataAccessLayer)

        println("done")

        if(args.first() == "openapi"){
            OpenApiCommand().parse(args)
        }else{
            Cli.loop(dataAccessLayer, config) //blocks until quit command is given
        }

        RestApi.stop()
        RunExecutor.stop()
        EventStreamProcessor.stop()
        FFmpegUtil.stop()
    }
}
