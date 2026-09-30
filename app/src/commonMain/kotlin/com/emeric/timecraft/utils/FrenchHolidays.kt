package com.emeric.timecraft.utils

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

object FrenchHolidays {
    fun getHolidaysForYear(year: Int): Map<LocalDate, String> {
        val map = mutableMapOf<LocalDate, String>()

        // Fixed French public holidays
        map[LocalDate(year, 1, 1)] = "Jour de l'An"
        map[LocalDate(year, 5, 1)] = "Fête du Travail"
        map[LocalDate(year, 5, 8)] = "Victoire 1945"
        map[LocalDate(year, 7, 14)] = "Fête Nationale"
        map[LocalDate(year, 8, 15)] = "Assomption"
        map[LocalDate(year, 11, 1)] = "Toussaint"
        map[LocalDate(year, 11, 11)] = "Armistice 1918"
        map[LocalDate(year, 12, 25)] = "Noël"

        // Variable Easter-based holidays
        val easter = getEasterDate(year)
        map[easter.plus(1, DateTimeUnit.DAY)] = "Lundi de Pâques"
        map[easter.plus(39, DateTimeUnit.DAY)] = "Ascension"
        map[easter.plus(50, DateTimeUnit.DAY)] = "Lundi de Pentecôte"

        return map
    }

    fun getHolidayName(date: LocalDate): String? {
        return getHolidaysForYear(date.year)[date]
    }

    fun isHoliday(date: LocalDate): Boolean {
        return getHolidayName(date) != null
    }

    private fun getEasterDate(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate(year, month, day)
    }
}
