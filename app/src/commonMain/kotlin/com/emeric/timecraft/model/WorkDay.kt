package com.emeric.timecraft.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate
import androidx.compose.ui.graphics.Color
import com.emeric.timecraft.SettingsStorage

@Serializable
enum class DayType(val label: String, val colorHex: String) {
    WORKED("Jour travaillé", "#1A3A5A"),
    RTT("RTT", "#FF9800"),
    PAID_LEAVE("Congés payé", "#4CAF50"),
    FAMILY_ABSENCE("Absence événement familial", "#9C27B0"),
    UNPAID_LEAVE("Sans solde", "#F44336"),
    NONE("Aucun", "#00000000");

    fun getColor() = Color(parseColor(colorHex))
}

private fun parseColor(colorString: String): Int {
    if (colorString.startsWith("#")) {
        var color = colorString.substring(1).toLong(16)
        if (colorString.length == 7) {
            color = color or 0x00000000ff000000L
        }
        return color.toInt()
    }
    throw IllegalArgumentException("Unknown color")
}

private fun parseMinutes(timeStr: String): Int? {
    if (timeStr.isBlank()) return null
    val clean = timeStr.trim().lowercase().replace("h", ":").replace(".", ":").replace(" ", "")
    val parts = clean.split(":")
    if (parts.isEmpty()) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = if (parts.size > 1) (parts[1].toIntOrNull() ?: 0) else 0
    return h * 60 + m
}

@Serializable
data class DayScheduleConfig(
    val dayOfWeekName: String,
    val label: String,
    val morningStart: String = "08:00",
    val morningEnd: String = "12:00",
    val afternoonStart: String = "13:30",
    val afternoonEnd: String = "17:30",
    val isWorkDay: Boolean = true
) {
    fun calculateTargetHours(): Double {
        if (!isWorkDay) return 0.0

        val mStart = parseMinutes(morningStart)
        val mEnd = parseMinutes(morningEnd)
        val aStart = parseMinutes(afternoonStart)
        val aEnd = parseMinutes(afternoonEnd)

        var totalMinutes = 0
        if (mStart != null && mEnd != null && mEnd > mStart) {
            totalMinutes += (mEnd - mStart)
        }
        if (aStart != null && aEnd != null && aEnd > aStart) {
            totalMinutes += (aEnd - aStart)
        }
        return totalMinutes / 60.0
    }

    fun formattedTargetHours(): String {
        val hrs = calculateTargetHours()
        val h = hrs.toInt()
        val m = ((hrs - h) * 60).toInt()
        return if (m == 0) "${h}h" else "${h}h${m.toString().padStart(2, '0')}"
    }
}

object DefaultSchedules {
    val defaultDays = listOf(
        DayScheduleConfig("MONDAY", "Lundi", "08:00", "12:00", "13:30", "17:30", true),
        DayScheduleConfig("TUESDAY", "Mardi", "08:00", "12:00", "13:30", "17:30", true),
        DayScheduleConfig("WEDNESDAY", "Mercredi", "08:00", "12:00", "13:30", "17:30", true),
        DayScheduleConfig("THURSDAY", "Jeudi", "08:00", "12:00", "13:30", "17:30", true),
        DayScheduleConfig("FRIDAY", "Vendredi", "08:00", "12:00", "13:30", "16:30", true),
        DayScheduleConfig("SATURDAY", "Samedi", "08:00", "12:00", "", "", false),
        DayScheduleConfig("SUNDAY", "Dimanche", "", "", "", "", false)
    )

    fun getScheduleForDay(dayName: String, storage: SettingsStorage): DayScheduleConfig {
        val defaultItem = defaultDays.firstOrNull { it.dayOfWeekName == dayName } ?: defaultDays.first()
        val jsonString = storage.getString("schedule_$dayName", "")
        if (jsonString.isBlank()) return defaultItem
        return try {
            Json.decodeFromString<DayScheduleConfig>(jsonString)
        } catch (e: Exception) {
            defaultItem
        }
    }

    fun saveScheduleForDay(config: DayScheduleConfig, storage: SettingsStorage) {
        val jsonString = Json.encodeToString(DayScheduleConfig.serializer(), config)
        storage.setString("schedule_${config.dayOfWeekName}", jsonString)
    }
}

@Serializable
data class ClientSchedule(
    @SerialName("client_name")
    val clientName: String = "",
    @SerialName("morning_start")
    val morningStart: String = "08:00",
    @SerialName("morning_end")
    val morningEnd: String = "12:00",
    @SerialName("afternoon_start")
    val afternoonStart: String = "13:30",
    @SerialName("afternoon_end")
    val afternoonEnd: String = "17:30"
) {
    fun toSerializedString(): String {
        return "$clientName|$morningStart|$morningEnd|$afternoonStart|$afternoonEnd"
    }

    companion object {
        fun fromSerializedString(str: String): ClientSchedule {
            val parts = str.split("|")
            return if (parts.size >= 5) {
                ClientSchedule(
                    clientName = parts[0],
                    morningStart = parts[1],
                    morningEnd = parts[2],
                    afternoonStart = parts[3],
                    afternoonEnd = parts[4]
                )
            } else {
                ClientSchedule(clientName = str)
            }
        }
    }

    fun calculateHours(): Double {
        val mStart = parseMinutes(morningStart)
        val mEnd = parseMinutes(morningEnd)
        val aStart = parseMinutes(afternoonStart)
        val aEnd = parseMinutes(afternoonEnd)

        var totalMinutes = 0
        if (mStart != null && mEnd != null && mEnd > mStart) {
            totalMinutes += (mEnd - mStart)
        }
        if (aStart != null && aEnd != null && aEnd > aStart) {
            totalMinutes += (aEnd - aStart)
        }
        return totalMinutes / 60.0
    }

    fun formattedHours(): String {
        val hrs = calculateHours()
        val h = hrs.toInt()
        val m = ((hrs - h) * 60).toInt()
        return if (m == 0) "${h}h" else "${h}h${m.toString().padStart(2, '0')}"
    }
}

@Serializable
data class WorkDay(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    val date: LocalDate,
    val type: DayType = DayType.WORKED,
    @SerialName("is_worked")
    val isWorked: Boolean = true,
    val expenses: List<Expense> = emptyList(),
    val clients: List<String> = emptyList()
) {
    fun getEffectiveSchedules(): List<ClientSchedule> {
        return clients.map { ClientSchedule.fromSerializedString(it) }
    }

    fun totalWorkedHours(): Double {
        if (type != DayType.WORKED) return 0.0
        return getEffectiveSchedules().sumOf { it.calculateHours() }
    }

    fun formattedTotalHours(): String {
        val hrs = totalWorkedHours()
        val h = hrs.toInt()
        val m = ((hrs - h) * 60).toInt()
        return if (m == 0) "${h}h" else "${h}h${m.toString().padStart(2, '0')}"
    }
}

@Serializable
data class Expense(
    val id: String? = null,
    val amount: Double,
    val description: String,
    val category: String = "Général"
)
