package com.emeric.timecraft.utils

import com.emeric.timecraft.LocationPoint
import com.emeric.timecraft.MapProjection
import com.emeric.timecraft.getSettingsStorage
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object GpsStorage {
    private val json = Json { ignoreUnknownKeys = true }
    private fun storage() = getSettingsStorage()

    fun getPointsForDate(dateStr: String): List<LocationPoint> {
        val key = "gps_track_$dateStr"
        val rawJson = storage().getString(key, "")
        if (rawJson.isBlank()) return emptyList()
        return try {
            json.decodeFromString<List<LocationPoint>>(rawJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePointsForDate(dateStr: String, points: List<LocationPoint>) {
        val key = "gps_track_$dateStr"
        val rawJson = json.encodeToString(points)
        storage().setString(key, rawJson)

        val datesKey = "gps_dates_list"
        val existingDatesJson = storage().getString(datesKey, "")
        val datesList = try {
            if (existingDatesJson.isNotBlank()) json.decodeFromString<List<String>>(existingDatesJson) else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        if (!datesList.contains(dateStr)) {
            val newDatesList = datesList + dateStr
            storage().setString(datesKey, json.encodeToString(newDatesList))
        }
    }

    fun addPointForDate(dateStr: String, point: LocationPoint): List<LocationPoint> {
        val currentPoints = getPointsForDate(dateStr)
        val updatedPoints = currentPoints + point
        savePointsForDate(dateStr, updatedPoints)
        return updatedPoints
    }

    fun getAllTrackedDates(): List<String> {
        val datesKey = "gps_dates_list"
        val existingDatesJson = storage().getString(datesKey, "")
        return try {
            if (existingDatesJson.isNotBlank()) json.decodeFromString<List<String>>(existingDatesJson) else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getPointsForPeriod(startDate: LocalDate, endDate: LocalDate): List<LocationPoint> {
        val datesList = getAllTrackedDates()
        val allPoints = mutableListOf<LocationPoint>()
        for (dateStr in datesList) {
            val date = try { LocalDate.parse(dateStr) } catch (e: Exception) { null }
            if (date != null && date >= startDate && date <= endDate) {
                allPoints.addAll(getPointsForDate(dateStr))
            }
        }
        return allPoints
    }

    fun calculateDistanceKm(points: List<LocationPoint>): Double {
        if (points.size < 2) return 0.0
        var totalMeters = 0.0
        for (i in 0 until points.size - 1) {
            totalMeters += MapProjection.distanceMeters(
                points[i].latitude, points[i].longitude,
                points[i + 1].latitude, points[i + 1].longitude
            )
        }
        return totalMeters / 1000.0
    }
}
