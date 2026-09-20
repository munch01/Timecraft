package com.example.timecraft.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.timecraft.model.WorkDay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

import com.example.timecraft.network.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import com.example.timecraft.model.DayType

class CalendarViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var currentMonth by mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.let { LocalDate(it.year, it.month, 1) })
        private set

    var workDays by mutableStateOf(mapOf<LocalDate, WorkDay>())
        private set

    init {
        fetchDays()
    }

    fun onPreviousMonth() {
        val prev = currentMonth.minus(1, DateTimeUnit.MONTH)
        currentMonth = LocalDate(prev.year, prev.month, 1)
        fetchDays()
    }

    fun onNextMonth() {
        val next = currentMonth.plus(1, DateTimeUnit.MONTH)
        currentMonth = LocalDate(next.year, next.month, 1)
        fetchDays()
    }

    private fun fetchDays() {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        scope.launch {
            try {
                val results = supabase.from("work_days")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<WorkDay>()
                workDays = results.associateBy { it.date }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveWorkDay(workDay: WorkDay) {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        val finalWorkDay = workDay.copy(userId = userId)
        
        workDays = workDays + (finalWorkDay.date to finalWorkDay)

        scope.launch {
            try {
                supabase.from("work_days").upsert(finalWorkDay)
            } catch (e: Exception) {
                e.printStackTrace()
                fetchDays() // Refresh on error
            }
        }
    }

    fun deleteWorkDay(date: LocalDate) {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        workDays = workDays - date

        scope.launch {
            try {
                supabase.from("work_days").delete {
                    filter {
                        eq("user_id", userId)
                        eq("date", date.toString())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                fetchDays()
            }
        }
    }

    // Compatibility for toggle if needed (defaults to WORKED)
    fun toggleDay(date: LocalDate) {
        if (workDays.containsKey(date)) {
            deleteWorkDay(date)
        } else {
            val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
            saveWorkDay(WorkDay(userId = userId, date = date, type = DayType.WORKED))
        }
    }
}
