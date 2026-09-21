package com.emeric.timecraft.utils

import kotlinx.datetime.*

object DateTimeUtils {
    fun getMonthName(month: Month): String {
        return when (month) {
            Month.JANUARY -> "Janvier"
            Month.FEBRUARY -> "Février"
            Month.MARCH -> "Mars"
            Month.APRIL -> "Avril"
            Month.MAY -> "Mai"
            Month.JUNE -> "Juin"
            Month.JULY -> "Juillet"
            Month.AUGUST -> "Août"
            Month.SEPTEMBER -> "Septembre"
            Month.OCTOBER -> "Octobre"
            Month.NOVEMBER -> "Novembre"
            Month.DECEMBER -> "Décembre"
        }
    }

    fun getDaysInMonth(year: Int, month: Month): List<LocalDate> {
        val firstDay = LocalDate(year, month, 1)
        val daysInMonth = mutableListOf<LocalDate>()
        var current = firstDay
        while (current.month == month) {
            daysInMonth.add(current)
            current = current.plus(1, DateTimeUnit.DAY)
        }
        return daysInMonth
    }
}
