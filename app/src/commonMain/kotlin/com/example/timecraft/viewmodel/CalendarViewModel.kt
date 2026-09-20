package com.example.timecraft.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.timecraft.model.WorkDay
import com.example.timecraft.model.DayType
import com.example.timecraft.network.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import com.example.timecraft.getPlatform

class CalendarViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    var currentMonth by mutableStateOf(
        getPlatform().getCurrentLocalDate().let { LocalDate(it.year, it.month, 1) }
    )
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
        val userId = try {
            supabase.auth.currentSessionOrNull()?.user?.id
        } catch (e: Exception) {
            null
        } ?: return
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
        val userId = try {
            supabase.auth.currentSessionOrNull()?.user?.id
        } catch (e: Exception) {
            null
        } ?: return
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
        val userId = try {
            supabase.auth.currentSessionOrNull()?.user?.id
        } catch (e: Exception) {
            null
        } ?: return
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

    fun toggleDay(date: LocalDate) {
        if (workDays.containsKey(date)) {
            deleteWorkDay(date)
        } else {
            val userId = try {
                supabase.auth.currentSessionOrNull()?.user?.id
            } catch (e: Exception) {
                null
            } ?: return
            saveWorkDay(WorkDay(userId = userId, date = date, type = DayType.WORKED))
        }
    }
}
