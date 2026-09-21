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
)

@Serializable
data class Expense(
    val id: String? = null,
    val amount: Double,
    val description: String,
    val category: String = "Général"
)
