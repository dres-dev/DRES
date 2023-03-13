package dev.dres

import dev.dres.api.cli.Cli
import dev.dres.api.cli.OpenApiCommand
import dev.dres.api.rest.RestApi
import dev.dres.data.model.Config
import dev.dres.data.model.admin.DbRole
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.audit.DbAuditLogEntry
import dev.dres.data.model.audit.DbAuditLogSource
import dev.dres.data.model.audit.DbAuditLogType
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.*
import dev.dres.data.model.template.task.options.*
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.template.team.DbTeamAggregator
import dev.dres.data.model.template.team.DbTeamGroup
import dev.dres.data.model.media.DbMediaCollection
import dev.dres.data.model.media.DbMediaItem
import dev.dres.data.model.media.DbMediaSegment
import dev.dres.data.model.media.DbMediaType
import dev.dres.data.model.run.DbEvaluation
import dev.dres.data.model.run.DbEvaluationType
import dev.dres.data.model.run.DbTask
import dev.dres.data.model.submissions.*
import dev.dres.run.RunExecutor
import dev.dres.run.eventstream.EventStreamProcessor
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

        /* Initialize RunExecutor. */
        RunExecutor.init(store)

        /* Initialize EventStreamProcessor */
        EventStreamProcessor.register( /* Add handlers here */)
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
            DbAuditLogEntry,
            DbAuditLogSource,
            DbAuditLogType,
            DbConfiguredOption,
            DbEvaluation,
            DbEvaluationType,
            DbEvaluationTemplate,
            DbHint,
            DbHintOption,
            DbHintType,
            DbMediaType,
            DbMediaCollection,
            DbMediaItem,
            DbMediaSegment,
            DbRole,
            DbScoreOption,
            DbSubmission,
            DbSubmissionOption,
            DbTask,
            DbTaskGroup,
            DbTaskType,
            DbTaskOption,
            DbTaskTemplate,
            DbTaskTemplateTarget,
            DbTargetType,
            DbTargetOption,
            DbTeam,
            DbTeamAggregator,
            DbTeamGroup,
            DbUser,
            DbAnswer,
            DbAnswerSet,
            DbAnswerType,
            DbVerdictStatus,
        )
        val store = StaticStoreContainer.init(dbFolder = File(config.dataPath), entityStoreName = "dres-db")
        initMetaData(XdModel.hierarchy, store)

        return store
    }
}
