package dev.dres

import dev.dres.api.cli.Cli
import dev.dres.api.cli.OpenApiCommand
import dev.dres.api.rest.RestApi
import dev.dres.data.model.admin.DbRole
import dev.dres.data.model.admin.DbUser
import dev.dres.data.model.config.Config
import dev.dres.data.model.media.*
import dev.dres.data.model.run.*
import dev.dres.data.model.submissions.*
import dev.dres.data.model.template.DbEvaluationTemplate
import dev.dres.data.model.template.task.*
import dev.dres.data.model.template.task.options.*
import dev.dres.data.model.template.team.DbTeam
import dev.dres.data.model.template.team.DbTeamAggregator
import dev.dres.data.model.template.team.DbTeamGroup
import dev.dres.mgmt.MediaCollectionManager
import dev.dres.mgmt.TemplateManager
import dev.dres.mgmt.admin.UserManager
import dev.dres.mgmt.cache.CacheManager
import dev.dres.run.RunExecutor
import dev.dres.run.audit.AuditLogger
import dev.dres.run.eventstream.EventStreamProcessor
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.XdModel
import kotlinx.dnq.store.container.StaticStoreContainer
import kotlinx.dnq.util.initMetaData
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess


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
    const val VERSION = "2.0.5-SNAPSHOT"

    /** Application root; should be relative to JAR file or classes path. */
    val APPLICATION_ROOT: Path =
        File(DRES::class.java.protectionDomain.codeSource.location.toURI()).toPath()


    /** The path to the data folder. Can be different from the application root if provided via command line argument */
    lateinit var DATA_ROOT: Path
        internal set

    /** Path to the directory that contains the external items. */
    lateinit var EXTERNAL_ROOT: Path
        internal set

    /** Path to the directory that contains the audit log. */
    lateinit var AUDIT_LOG_ROOT: Path
        internal set

    /** Path to the directory that contains task type presets. */
    lateinit var TASK_TYPE_PRESETS_EXTERNAL_LOCATION: Path
        internal set

    /** Path to the classpath directory that contains task type presets shipped with DRES. */
    const val TASK_TYPE_PRESETS_LOCATION = "dres-type-presets"

    /** The config loaded */
    lateinit var CONFIG : Config
        internal set

    init {
        //redirect log of JLine3 from jdk logger to log4j
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        CONFIG = if (args.isNotEmpty()) {
            try{
                val configPath = Paths.get(args[0])
                val config = Config.read(configPath)
                config
            }catch(e: Exception){
                println("ERROR: The config at ${Paths.get(args[0]).toAbsolutePath()} could not be loaded. See stacktrace for more information.")
                exitProcess(11)
            }
        } else {
            Config()
        }

        EXTERNAL_ROOT = CONFIG.externalMediaLocation
        TASK_TYPE_PRESETS_EXTERNAL_LOCATION = CONFIG.presetsLocation
        DATA_ROOT = CONFIG.dataPath
        AUDIT_LOG_ROOT = CONFIG.auditLocation

        println("Starting DRES (application: $APPLICATION_ROOT, data: $DATA_ROOT)")
        println("Initializing...")



        /* Initialize Xodus based data store. */
        val store = this.prepareDatabase()

        /* Initialize the global Cache Manager. */
        val global = CacheManager(CONFIG, store)

        /* Initialize data managers */
        UserManager.init(store)
        MediaCollectionManager.init(store)
        TemplateManager.init(store)

        /* Initialize RunExecutor. */
        RunExecutor.init(store)

        /* Initialize EventStreamProcessor */
        EventStreamProcessor.register( /* Add handlers here */)
        EventStreamProcessor.init()

        /* Initialize Rest API. */
        RestApi.init(CONFIG, store, global)

        AuditLogger.startup()

        println("Initialization complete!")

        if (args.isNotEmpty() && args.first() == "openapi") {
            OpenApiCommand().parse(args)
        } else {
            Cli.loop(CONFIG, store, global) //blocks until quit command is given
        }

        /* Stop. */
        global.stop()
        RestApi.stop()
        RunExecutor.stop()
        EventStreamProcessor.stop()
        AuditLogger.stop()
    }

    /**
     * Loads and prepares the database.
     */
    private fun prepareDatabase(): TransientEntityStore  {
        XdModel.registerNodes(
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
            DbMediaItemMetaDataEntry,
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
        val store = StaticStoreContainer.init(dbFolder = DATA_ROOT.toFile(), entityStoreName = "dres-db")
        initMetaData(XdModel.hierarchy, store)
        return store
    }
}
