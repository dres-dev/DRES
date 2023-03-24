package dev.dres

import dev.dres.api.cli.Cli
import dev.dres.api.cli.OpenApiCommand
import dev.dres.api.rest.RestApi
import dev.dres.data.model.config.Config
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
import dev.dres.data.model.run.*
import dev.dres.data.model.submissions.*
import dev.dres.mgmt.cache.CacheManager
import dev.dres.run.RunExecutor
import dev.dres.run.eventstream.EventStreamProcessor
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.XdModel
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute


/**
 * Main class of DRES with application entry point.
 *
 * @author Ralph Gasser
 * @author Luca Rossetto
 * @author Loris Sauter
 * @version 2.0.0
 */
object DRES {
    /** Version of DRES. */
    const val VERSION = "2.0.0"

    /** Application root; should pe relative to JAR file or classes path. */
    val APPLICATION_ROOT: Path = File(DRES::class.java.protectionDomain.codeSource.location.toURI()).toPath()

    /** The path to the data folder. Can be different from the application root if provided via command line argument */
    var DATA_ROOT: Path = APPLICATION_ROOT

    /** Path to the directory that contains the external items. */
    val EXTERNAL_ROOT: Path
        get() = DATA_ROOT.resolve("external")

    init {
        //redirect log of JLine3 from jdk logger to log4j
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val config = if (args.isNotEmpty()) {
            val configPath = Paths.get(args[0])
            val config = Config.read(configPath)
            DATA_ROOT = configPath.absolute().parent
            config
        } else {
            Config()
        }

        println("Starting DRES (application: $APPLICATION_ROOT, data: $DATA_ROOT)")
        println("Initializing...")

        /* Initialize Xodus based data store. */
        val store = this.prepareDatabase(config)

        /* Initialize the global Cache Manager. */
        val global = CacheManager(config, store)

        /* Initialize RunExecutor. */
        RunExecutor.init(config, store, global)

        /* Initialize EventStreamProcessor */
        EventStreamProcessor.register( /* Add handlers here */)
        EventStreamProcessor.init()

        /* Initialize Rest API. */
        RestApi.init(config, store, global)

        println("Initialization complete!")

        if (args.isNotEmpty() && args.first() == "openapi") {
            OpenApiCommand().parse(args)
        } else {
            Cli.loop(config, store, global) //blocks until quit command is given
        }

        /* Stop. */
        global.stop()
        RestApi.stop()
        RunExecutor.stop()
        EventStreamProcessor.stop()
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
            DbEvaluationStatus,
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
            DbTaskStatus,
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
        val store = StaticStoreContainer.init(dbFolder = DATA_ROOT.resolve("data").toFile(), entityStoreName = "dres-db")
        initMetaData(XdModel.hierarchy, store)
        return store
    }
}
