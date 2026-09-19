package com.example.timecraft.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.timecraft.model.WorkDay
import kotlinx.datetime.*

class CalendarViewModel {
    var currentMonth by mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.let { LocalDate(it.year, it.month, 1) })
        private set

    var workedDays by mutableStateOf(setOf<LocalDate>())
        private set

    fun onPreviousMonth() {
        val prev = currentMonth.minus(1, DateTimeUnit.MONTH)
        currentMonth = LocalDate(prev.year, prev.month, 1)
    }

    fun onNextMonth() {
        val next = currentMonth.plus(1, DateTimeUnit.MONTH)
        currentMonth = LocalDate(next.year, next.month, 1)
    }

    fun toggleDay(date: LocalDate) {
        workedDays = if (workedDays.contains(date)) {
            workedDays - date
        } else {
            workedDays + date
        }
        // TODO: Sync with Supabase
    }
}
