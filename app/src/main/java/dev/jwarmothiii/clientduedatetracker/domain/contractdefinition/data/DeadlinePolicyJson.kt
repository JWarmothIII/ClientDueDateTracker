package dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.data

import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.ContractEventKind
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.DeadlineAnchor
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.DeadlinePolicy
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.RecurrencePolicy
import dev.jwarmothiii.clientduedatetracker.domain.contractdefinition.model.WeekendAdjustment

class DataIntegrityException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

object DeadlinePolicyJson {
    private const val CURRENT_VERSION = 1

    fun encode(policy: DeadlinePolicy): String =
        buildString {
            append("""{"version":$CURRENT_VERSION""")
            append(""","anchor":"${policy.anchor.name}"""")
            append(""","offsetDays":${policy.offsetDays}""")
            append(""","dayNumber":${policy.dayNumber ?: "null"}""")
            append(""","monthOffset":${policy.monthOffset ?: "null"}""")
            append(""","endOfMonth":${policy.endOfMonth}""")
            append(""","recurrence":"${policy.recurrence.name}"""")
            append(""","eventKind":${policy.eventKind?.let { "\"${it.name}\"" } ?: "null"}""")
            append(""","weekendAdjustment":"${policy.weekendAdjustment.name}"}""")
        }

    fun decode(json: String): DeadlinePolicy =
        try {
            val values =
                FIELD_REGEX
                    .findAll(json)
                    .associate { match -> match.groupValues[1] to match.groupValues[2].trim('"') }
            val version = values.required("version").toInt()
            if (version != CURRENT_VERSION) {
                throw DataIntegrityException("Unsupported deadline-policy version: $version")
            }
            DeadlinePolicy(
                anchor = enumValue(values.required("anchor"), "anchor"),
                offsetDays = values.required("offsetDays").toInt(),
                dayNumber = values.optionalInt("dayNumber"),
                monthOffset = values.optionalInt("monthOffset"),
                endOfMonth = values.required("endOfMonth").toBooleanStrict(),
                recurrence = enumValue(values.required("recurrence"), "recurrence"),
                eventKind = values.optionalEnum<ContractEventKind>("eventKind"),
                weekendAdjustment = enumValue(values.required("weekendAdjustment"), "weekendAdjustment"),
            )
        } catch (exception: DataIntegrityException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw DataIntegrityException("Corrupt deadline-policy JSON.", exception)
        }

    private inline fun <reified T : Enum<T>> enumValue(
        value: String,
        field: String,
    ): T =
        enumValues<T>().firstOrNull { it.name == value }
            ?: throw DataIntegrityException("Unknown $field code: $value")

    private inline fun <reified T : Enum<T>> Map<String, String>.optionalEnum(key: String): T? =
        get(key)?.takeUnless { it == "null" }?.let { enumValue<T>(it, key) }

    private fun Map<String, String>.required(key: String): String =
        get(key) ?: throw DataIntegrityException("Missing deadline-policy field: $key")

    private fun Map<String, String>.optionalInt(key: String): Int? = required(key).takeUnless { it == "null" }?.toInt()

    private val FIELD_REGEX = """"([^"]+)"\s*:\s*("[^"]*"|-?\d+|true|false|null)""".toRegex()
}
