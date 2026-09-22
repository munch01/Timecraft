package com.emeric.timecraft.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.emeric.timecraft.model.WorkDay
import com.emeric.timecraft.model.DayType
import com.emeric.timecraft.network.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import com.emeric.timecraft.getPlatform
import kotlinx.serialization.json.Json

class CalendarViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var currentMonth by mutableStateOf(
        getPlatform().getCurrentLocalDate().let { LocalDate(it.year, it.month, 1) }
    )
        private set

    var workDays by mutableStateOf(mapOf<LocalDate, WorkDay>())
        private set

    init {
        fetchDays()
    }

    fun refresh() {
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
        scope.launch {
            try {
                var userId: String? = null
                for (i in 1..10) {
                    userId = supabase.auth.currentSessionOrNull()?.user?.id
                    if (userId != null) break
                    kotlinx.coroutines.delay(300)
                }
                
                if (userId == null) return@launch

                val rawData = withContext(Dispatchers.Default) {
                    supabase.from("work_days")
                        .select {
                            filter {
                                eq("user_id", userId)
                            }
                        }.data
                }

                val jsonParser = Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    isLenient = true
                }

                val results = withContext(Dispatchers.Default) {
                    jsonParser.decodeFromString<List<WorkDay>>(rawData)
                }

                // Update Compose state on Main UI thread
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
        
        // Ensure we preserve the ID if we already know it for this date
        val existingId = workDays[workDay.date]?.id
        val finalWorkDay = workDay.copy(userId = userId, id = existingId)
        
        workDays = workDays + (finalWorkDay.date to finalWorkDay)

        scope.launch {
            try {
                withContext(Dispatchers.Default) {
                    supabase.from("work_days").upsert(finalWorkDay) {
                        onConflict = "user_id,date"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                withContext(Dispatchers.Default) {
                    supabase.from("work_days").delete {
                        filter {
                            eq("user_id", userId)
                            eq("date", date.toString())
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
