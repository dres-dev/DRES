package dev.dres.api.cli

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.dres.data.model.audit.DbAuditLogEntry
import jetbrains.exodus.database.TransientEntityStore
import kotlinx.dnq.query.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.io.File


class AuditCommand(private val store: TransientEntityStore) : NoOpCliktCommand(name = "audit") {

    init {
        subcommands(Show(), Export())
    }

    inner class Show() :
        CliktCommand(name = "show", help = "Shows latest audit log entries", printHelpOnEmptyArgs = false) {

        private val number: Int by option("-n", "--number", help = "Number of audit log entries to show")
            .convert { it.toInt() }
            .default(10)

        override fun run() {
            this@AuditCommand.store.transactional(readonly = true) {
                DbAuditLogEntry.all().sortedBy(DbAuditLogEntry::timestamp, asc = false)
                    .take(number).asSequence().forEach { entry ->
                        println("${entry.timestamp}: (${entry.type.description}@${entry.source.description}) user: ${entry.userId}, evaluation: ${entry.evaluationId}, task: ${entry.taskId}, description: ${entry.description}")
                    }
            }
        }
    }

    inner class Export() : CliktCommand(
        name = "export",
        help = "Exports audit log entries from within a specified time frame to a file as one JSON object per line",
        printHelpOnEmptyArgs = true
    ) {

        /** The output path for the export. */
        private val output: File by option(
            "-o",
            "--output",
            help = "Path of the file the media collection should to be exported to."
        ).convert { File(it) }.required()

        private val pattern = DateTimeFormat.forPattern("dd.MM.yyyy-HH:mm:ss")

        private val startTimeStamp: DateTime by option(
            "--from",
            help = "start time stamp from which to export in the format dd.MM.yyyy-HH:mm:ss"
        ).convert {
            DateTime.parse(it, pattern)
        }.default(DateTime(0))

        private val endTimeStamp: DateTime by option(
            "--to",
            help = "end time stamp from which to export in the format dd.MM.yyyy-HH:mm:ss"
        ).convert {
            DateTime.parse(it, pattern)
        }.default(DateTime(Long.MAX_VALUE))

        override fun run() {
            val writer = output.printWriter(Charsets.UTF_8)
            val mapper = jacksonObjectMapper()

            var counter = 0

            this@AuditCommand.store.transactional(readonly = true) {
                DbAuditLogEntry.query((DbAuditLogEntry::timestamp ge startTimeStamp) and (DbAuditLogEntry::timestamp le endTimeStamp))
                    .sortedBy(DbAuditLogEntry::timestamp, asc = true)
                    .asSequence().forEach {
                        writer.println(mapper.writeValueAsString(it.toApi()))
                        ++counter
                    }
            }

            writer.flush()
            writer.close()

            println("wrote $counter entries to ${output.absolutePath}")

        }

    }


}