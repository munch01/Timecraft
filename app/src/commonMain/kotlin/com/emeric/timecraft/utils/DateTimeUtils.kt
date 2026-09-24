package com.emeric.timecraft.utils

import kotlinx.datetime.*

object DateTimeUtils {
    fun getMonthName(month: Month, language: AppLanguage = LanguageManager.currentLanguage): String {
        return when (language) {
            AppLanguage.FRENCH -> when (month) {
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
            AppLanguage.ENGLISH -> when (month) {
                Month.JANUARY -> "January"
                Month.FEBRUARY -> "February"
                Month.MARCH -> "March"
                Month.APRIL -> "April"
                Month.MAY -> "May"
                Month.JUNE -> "June"
                Month.JULY -> "July"
                Month.AUGUST -> "August"
                Month.SEPTEMBER -> "September"
                Month.OCTOBER -> "October"
                Month.NOVEMBER -> "November"
                Month.DECEMBER -> "December"
            }
            AppLanguage.SPANISH -> when (month) {
                Month.JANUARY -> "Enero"
                Month.FEBRUARY -> "Febrero"
                Month.MARCH -> "Marzo"
                Month.APRIL -> "Abril"
                Month.MAY -> "Mayo"
                Month.JUNE -> "Junio"
                Month.JULY -> "Julio"
                Month.AUGUST -> "Agosto"
                Month.SEPTEMBER -> "Septiembre"
                Month.OCTOBER -> "Octubre"
                Month.NOVEMBER -> "Noviembre"
                Month.DECEMBER -> "Diciembre"
            }
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
