package com.emeric.timecraft

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
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
                val activity = currentActivity
                if (activity != null) {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        1001
                    )
                } else {
                    getPlatform().openAppSettings()
                }
                getPlatform().showToast("Veuillez autoriser la géolocalisation dans le pop-up système")
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
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, this)
                    registered = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 5f, this)
                    registered = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 5000L, 5f, this)
                registered = true
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (registered) {
                _isTracking.value = true
                LocationForegroundService.start(context)

                // Query last known position from all providers
                val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                var foundLoc: Location? = null
                for (p in providers) {
                    try {
                        val loc = locationManager.getLastKnownLocation(p)
                        if (loc != null) {
                            if (foundLoc == null || loc.time > foundLoc.time) {
                                foundLoc = loc
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (foundLoc != null && (!foundLoc.hasAccuracy() || foundLoc.accuracy <= 25.0f)) {
                    onLocationChanged(foundLoc)
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
        // 1. Accuracy Filter: Ignore imprecise locations (> 25m accuracy radius)
        if (location.hasAccuracy() && location.accuracy > 25.0f) {
            return
        }

        // 2. Extra strict accuracy check for network/passive provider
        if (location.provider == LocationManager.NETWORK_PROVIDER && location.hasAccuracy() && location.accuracy > 15.0f) {
            return
        }

        val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
        val point = LocationPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = location.time,
            speedKmh = speedKmh
        )

        // Always update the current location marker for the UI
        _currentPoint.value = point

        val currentList = _trackHistory.value
        val lastPoint = currentList.lastOrNull()

        if (lastPoint == null) {
            _trackHistory.value = listOf(point)
            return
        }

        val distMeters = MapProjection.distanceMeters(
            lastPoint.latitude, lastPoint.longitude,
            point.latitude, point.longitude
        )

        val timeDeltaSec = (location.time - lastPoint.timestamp) / 1000.0

        // 3. Outlier / Teleportation Jump Filter
        // Ignore impossible speed spikes (> 140 km/h or > 120m jump in < 3s)
        if (timeDeltaSec > 0) {
            val impliedSpeedKmh = (distMeters / timeDeltaSec) * 3.6
            if (impliedSpeedKmh > 140.0) {
                return
            }
        }
        if (timeDeltaSec < 3.0 && distMeters > 120.0) {
            return
        }

        // 4. Anti-Drift Stationary Filter (office / desk / red light)
        // Require a higher movement threshold if speed is low or static (< 3 km/h)
        val minRequiredDistance = if (speedKmh < 3.0f) 15.0 else 8.0

        if (distMeters >= minRequiredDistance) {
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
