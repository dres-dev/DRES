package dev.dres.api.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dres.data.model.audit.DbAuditLogEntry
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.asSequence
import kotlinx.dnq.query.sortedBy
import kotlinx.dnq.query.take

class AuditCommand(private val store: TransientEntityStore) : NoOpCliktCommand(name = "audit") {

    init {
        subcommands(Show())
    }

    inner class Show() : CliktCommand(name = "shows", help = "Shows latest audit log entries", printHelpOnEmptyArgs = true) {

        private val number: Int by option("-n", "--number", help = "Number of audit log entries to show")
                .convert { it.toInt() }
                .default(10)

        override fun run() {

            this@AuditCommand.store.transactional(readonly = true) {
                DbAuditLogEntry.all().sortedBy(DbAuditLogEntry::timestamp, asc = true)
                        .take(number).asSequence().forEach { entry ->
                    println("${entry.timestamp}: (${entry.type.description}@${entry.source.description}) user: ${entry.userId}, evaluation: ${entry.evaluationId}, task: ${entry.taskId}, description: ${entry.description}")
                }
            }
        }
    }

}