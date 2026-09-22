package com.emeric.timecraft.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.emeric.timecraft.getPlatform
import com.emeric.timecraft.getSettingsStorage
import com.emeric.timecraft.model.DayType
import com.emeric.timecraft.model.WorkDay
import com.emeric.timecraft.network.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CalendarViewModel {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val storage = getSettingsStorage()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    var currentMonth by mutableStateOf(
        getPlatform().getCurrentLocalDate().let { LocalDate(it.year, it.month, 1) }
    )
        private set

    var workDays by mutableStateOf(mapOf<LocalDate, WorkDay>())
        private set

    init {
        workDays = loadLocalWorkDays()
        fetchDays()
    }

    fun refresh() {
        fetchDays()
    }

    private fun loadLocalWorkDays(): Map<LocalDate, WorkDay> {
        val jsonStr = storage.getString("cached_work_days", "")
        if (jsonStr.isBlank()) return emptyMap()
        return try {
            val list = jsonParser.decodeFromString<List<WorkDay>>(jsonStr)
            list.associateBy { it.date }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveLocalWorkDays(map: Map<LocalDate, WorkDay>) {
        try {
            val jsonStr = jsonParser.encodeToString(map.values.toList())
            storage.setString("cached_work_days", jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

                val results = withContext(Dispatchers.Default) {
                    jsonParser.decodeFromString<List<WorkDay>>(rawData)
                }

                val fetchedMap = results.associateBy { it.date }
                val merged = workDays + fetchedMap
                workDays = merged
                saveLocalWorkDays(merged)
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
        
        val existingId = workDays[workDay.date]?.id
        val finalWorkDay = workDay.copy(userId = userId, id = existingId)
        
        val updatedMap = workDays + (finalWorkDay.date to finalWorkDay)
        workDays = updatedMap
        saveLocalWorkDays(updatedMap)

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
        
        val updatedMap = workDays - date
        workDays = updatedMap
        saveLocalWorkDays(updatedMap)

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
