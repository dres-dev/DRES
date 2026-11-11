package dev.dres.api.cli

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import dev.dres.api.rest.types.users.ApiRole
import dev.dres.api.rest.types.users.ApiUserRequest
import dev.dres.data.model.admin.DbUser
import java.io.InputStream

/** User row parsed from the documented CSV import format. */
data class CsvUserImportRecord(val rowNumber: Int, val user: ApiUserRequest)

/** Parses and validates CSV user records without touching the user database. */
internal object UserCsvImport {
    private val requiredHeaders = setOf("username", "password", "role")

    /** Parses rows with required `username,password,role` headers; row numbers refer to source CSV lines. */
    fun read(input: InputStream): List<CsvUserImportRecord> {
        val rows = csvReader().readAllWithHeader(input)
        require(rows.isNotEmpty()) { "CSV contains no user rows." }

        val headers = rows.first().keys.map { it.trim().lowercase() }.toSet()
        val missingHeaders = requiredHeaders - headers
        require(missingHeaders.isEmpty()) {
            "CSV is missing required column(s): ${missingHeaders.sorted().joinToString(", ")}. Required headers: username,password,role"
        }

        val records = rows.mapIndexed { index, row ->
            val values = row.entries.associate { it.key.trim().lowercase() to it.value }
            val rowNumber = index + 2 // account for the header row
            val username = values["username"]?.trim()?.lowercase().orEmpty()
            val password = values["password"].orEmpty()
            val roleValue = values["role"]?.trim().orEmpty()

            require(username.length in DbUser.MIN_LENGTH_USERNAME..DbUser.MAX_LENGTH_USERNAME) {
                "CSV row $rowNumber: username must contain between ${DbUser.MIN_LENGTH_USERNAME} and ${DbUser.MAX_LENGTH_USERNAME} characters"
            }
            require(password.length >= DbUser.MIN_LENGTH_PASSWORD) {
                "CSV row $rowNumber: password must contain at least ${DbUser.MIN_LENGTH_PASSWORD} characters"
            }
            val role = try {
                ApiRole.parse(roleValue)
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("CSV row $rowNumber: invalid role '$roleValue'", error)
            }
            require(role != ApiRole.ANYONE) { "CSV row $rowNumber: ANYONE is not a valid user role" }

            CsvUserImportRecord(rowNumber, ApiUserRequest(username, password, role))
        }
        val duplicateNames = records.groupBy { it.user.username.lowercase() }.filterValues { it.size > 1 }.keys
        require(duplicateNames.isEmpty()) {
            "CSV contains duplicate usernames: ${duplicateNames.sorted().joinToString(", ")}"
        }
        return records
    }
}
