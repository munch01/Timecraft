package com.emeric.timecraft.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.datetime.LocalDate
import androidx.compose.ui.graphics.Color

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
    fun calculateHours(): Double {
        fun parseMinutes(timeStr: String): Int? {
            val clean = timeStr.trim().replace("h", ":").replace("H", ":")
            val parts = clean.split(":")
            if (parts.size != 2) return null
            val h = parts[0].trim().toIntOrNull() ?: return null
            val m = parts[1].trim().toIntOrNull() ?: return null
            return h * 60 + m
        }

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
    val clients: List<String> = emptyList(),
    @SerialName("client_schedules")
    val clientSchedules: List<ClientSchedule> = emptyList()
) {
    fun totalWorkedHours(): Double {
        if (type != DayType.WORKED) return 0.0
        return clientSchedules.sumOf { it.calculateHours() }
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
