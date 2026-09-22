package com.emeric.timecraft

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidLocationTracker(private val context: Context) : LocationTracker, LocationListener {
    private val _isTracking = MutableStateFlow(false)
    override val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _currentPoint = MutableStateFlow<LocationPoint?>(null)
    override val currentPoint: StateFlow<LocationPoint?> = _currentPoint.asStateFlow()

    private val _trackHistory = MutableStateFlow<List<LocationPoint>>(emptyList())
    override val trackHistory: StateFlow<List<LocationPoint>> = _trackHistory.asStateFlow()

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    override fun startTracking() {
        if (_isTracking.value) return
        try {
            val hasFine = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarse = context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasFine && !hasCoarse) {
                getPlatform().showToast("Permission localisation non accordée")
                return
            }

            // Battery optimized parameters: 15s interval, 15m displacement
            val provider = when {
                locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true -> LocationManager.GPS_PROVIDER
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true -> LocationManager.NETWORK_PROVIDER
                else -> null
            }

            if (provider != null) {
                locationManager?.requestLocationUpdates(
                    provider,
                    15000L, // 15 seconds
                    15f,    // 15 meters
                    this
                )
                _isTracking.value = true

                // Start Foreground Service to keep GPS active in background
                LocationForegroundService.start(context)

                // Try getting last known location immediately
                val lastLoc = locationManager?.getLastKnownLocation(provider)
                if (lastLoc != null) {
                    onLocationChanged(lastLoc)
                }
            } else {
                getPlatform().showToast("Localisation désactivée sur l'appareil")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getPlatform().showToast("Erreur GPS : ${e.message}")
        }
    }

    override fun stopTracking() {
        if (!_isTracking.value) return
        try {
            locationManager?.removeUpdates(this)
            LocationForegroundService.stop(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _isTracking.value = false
    }

    override fun clearHistory() {
        _trackHistory.value = emptyList()
    }

    override fun onLocationChanged(location: Location) {
        val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
        val point = LocationPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = location.time,
            speedKmh = speedKmh
        )
        _currentPoint.value = point

        val currentList = _trackHistory.value
        val lastPoint = currentList.lastOrNull()

        // Filter out duplicate or static noise points (< 10 meters) to preserve battery & route clean
        if (lastPoint == null || MapProjection.distanceMeters(lastPoint.latitude, lastPoint.longitude, point.latitude, point.longitude) >= 10.0) {
            _trackHistory.value = currentList + point
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}

private var singletonTracker: AndroidLocationTracker? = null

actual fun getLocationTracker(): LocationTracker {
    if (singletonTracker == null) {
        singletonTracker = AndroidLocationTracker(appContext)
    }
    return singletonTracker!!
}
