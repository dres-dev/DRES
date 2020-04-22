package dres.run.audit

import dres.data.dbo.DAO
import java.util.concurrent.ConcurrentHashMap

object AuditLogManager {

    private val loggers = ConcurrentHashMap<String, AuditLogger>()

    /**
     * gets the [AuditLogger] for a competition run or creates it if it does not already exist
     */
    @Synchronized
    fun getAuditLogger(competitionRun: String, dao: DAO<AuditLogEntry>): AuditLogger {
        if (!loggers.containsKey(competitionRun)){
            loggers[competitionRun] = AuditLogger(competitionRun, dao)
        }
        return loggers[competitionRun]!!
    }

    /**
     * gets the [AuditLogger] for a competition run if it exists
     */
    @Synchronized
    fun getAuditLogger(competitionRun: String): AuditLogger? = loggers[competitionRun]



}