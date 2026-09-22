package com.emeric.timecraft

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull

class DesktopLocationTracker : LocationTracker {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var trackingJob: Job? = null

    private val _isTracking = MutableStateFlow(false)
    override val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _currentPoint = MutableStateFlow<LocationPoint?>(null)
    override val currentPoint: StateFlow<LocationPoint?> = _currentPoint.asStateFlow()

    private val _trackHistory = MutableStateFlow<List<LocationPoint>>(emptyList())
    override val trackHistory: StateFlow<List<LocationPoint>> = _trackHistory.asStateFlow()

    private var baseLat = 48.8566 // Default Paris if IP fails
    private var baseLon = 2.3522

    override fun startTracking() {
        if (_isTracking.value) return
        _isTracking.value = true

        trackingJob = scope.launch {
            fetchIpLocation()
            var step = 0
            while (_isTracking.value) {
                // On PC Windows, simulate slight motion/route updates around IP location
                val latOffset = (step * 0.0005)
                val lonOffset = (step * 0.0008)
                val p = LocationPoint(
                    latitude = baseLat + latOffset,
                    longitude = baseLon + lonOffset,
                    timestamp = System.currentTimeMillis(),
                    speedKmh = 25f
                )
                _currentPoint.value = p
                _trackHistory.value = _trackHistory.value + p
                step++
                delay(15000) // 15 seconds interval
            }
        }
    }

    private suspend fun fetchIpLocation() {
        try {
            val client = HttpClient()
            val response = client.get("http://ip-api.com/json")
            val text = response.bodyAsText()
            val obj = Json.parseToJsonElement(text).jsonObject
            val lat = obj["lat"]?.jsonPrimitive?.doubleOrNull
            val lon = obj["lon"]?.jsonPrimitive?.doubleOrNull
            if (lat != null && lon != null) {
                baseLat = lat
                baseLon = lon
            }
            client.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun stopTracking() {
        _isTracking.value = false
        trackingJob?.cancel()
        trackingJob = null
    }

    override fun clearHistory() {
        _trackHistory.value = emptyList()
    }
}

private val desktopTracker = DesktopLocationTracker()

actual fun getLocationTracker(): LocationTracker = desktopTracker
