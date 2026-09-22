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

            if (locationManager == null) {
                getPlatform().showToast("Service de localisation indisponible")
                return
            }

            var registered = false

            // Register all available providers safely
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000L, 10f, this)
                    registered = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000L, 10f, this)
                    registered = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 10000L, 10f, this)
                registered = true
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (registered) {
                _isTracking.value = true
                LocationForegroundService.start(context)

                // Query last known position from any provider
                val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                for (p in providers) {
                    try {
                        val loc = locationManager.getLastKnownLocation(p)
                        if (loc != null) {
                            onLocationChanged(loc)
                            break
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                getPlatform().showToast("Veuillez activer le GPS / Localisation dans les paramètres de votre téléphone")
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

        // Filter out duplicate or static noise points (< 10 meters)
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
