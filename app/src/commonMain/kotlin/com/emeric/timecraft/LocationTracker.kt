package com.emeric.timecraft

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = 0L,
    val speedKmh: Float = 0f
)

interface LocationTracker {
    val isTracking: StateFlow<Boolean>
    val currentPoint: StateFlow<LocationPoint?>
    val trackHistory: StateFlow<List<LocationPoint>>

    fun startTracking()
    fun stopTracking()
    fun clearHistory()
}

expect fun getLocationTracker(): LocationTracker

object MapProjection {
    fun lonToTileX(lon: Double, zoom: Int): Double {
        return (lon + 180.0) / 360.0 * (1 shl zoom)
    }

    fun latToTileY(lat: Double, zoom: Int): Double {
        val latRad = kotlin.math.ln(kotlin.math.tan(Math.toRadians(lat)) + 1.0 / kotlin.math.cos(Math.toRadians(lat)))
        return (1.0 - latRad / Math.PI) / 2.0 * (1 shl zoom)
    }

    fun tileXToLon(x: Double, zoom: Int): Double {
        return x / (1 shl zoom) * 360.0 - 180.0
    }

    fun tileYToLat(y: Double, zoom: Int): Double {
        val n = Math.PI - 2.0 * Math.PI * y / (1 shl zoom)
        return Math.toDegrees(kotlin.math.atan(sinh(n)))
    }

    private fun sinh(x: Double): Double {
        return (kotlin.math.exp(x) - kotlin.math.exp(-x)) / 2.0
    }

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }
}
