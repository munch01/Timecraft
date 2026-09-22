package com.emeric.timecraft.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emeric.timecraft.LocationPoint
import com.emeric.timecraft.MapProjection
import com.emeric.timecraft.getLocationTracker
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    modifier: Modifier = Modifier
) {
    val locationTracker = remember { getLocationTracker() }
    val isTracking by locationTracker.isTracking.collectAsState()
    val currentPoint by locationTracker.currentPoint.collectAsState()
    val trackHistory by locationTracker.trackHistory.collectAsState()

    var mapCenterLat by remember { mutableStateOf(48.8566) } // Paris default
    var mapCenterLon by remember { mutableStateOf(2.3522) }
    var zoomLevel by remember { mutableStateOf(14) } // Zoom 1 to 18

    // Auto-center map on first point or when tracking starts
    LaunchedEffect(currentPoint) {
        currentPoint?.let {
            if (trackHistory.size <= 1) {
                mapCenterLat = it.latitude
                mapCenterLon = it.longitude
            }
        }
    }

    // Total distance calculation
    val totalDistanceKm = remember(trackHistory) {
        if (trackHistory.size < 2) 0.0
        else {
            var dist = 0.0
            for (i in 0 until trackHistory.size - 1) {
                val p1 = trackHistory[i]
                val p2 = trackHistory[i + 1]
                dist += MapProjection.distanceMeters(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
            }
            dist / 1000.0
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE8ECEF))
    ) {
        // Interactive Map Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(zoomLevel) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        // Pinch Zoom
                        if (zoomChange > 1.05f && zoomLevel < 18) {
                            zoomLevel += 1
                        } else if (zoomChange < 0.95f && zoomLevel > 3) {
                            zoomLevel -= 1
                        }

                        // Pan / Drag Map
                        val tileX = MapProjection.lonToTileX(mapCenterLon, zoomLevel)
                        val tileY = MapProjection.latToTileY(mapCenterLat, zoomLevel)

                        val deltaTileX = pan.x / 256.0
                        val deltaTileY = pan.y / 256.0

                        val newTileX = tileX - deltaTileX
                        val newTileY = tileY - deltaTileY

                        mapCenterLon = MapProjection.tileXToLon(newTileX, zoomLevel)
                        mapCenterLat = MapProjection.tileYToLat(newTileY, zoomLevel)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val centerTileX = MapProjection.lonToTileX(mapCenterLon, zoomLevel)
                val centerTileY = MapProjection.latToTileY(mapCenterLat, zoomLevel)

                fun pointToOffset(lat: Double, lon: Double): Offset {
                    val pTileX = MapProjection.lonToTileX(lon, zoomLevel)
                    val pTileY = MapProjection.latToTileY(lat, zoomLevel)

                    val offsetX = (pTileX - centerTileX) * 256.0 + (canvasWidth / 2.0)
                    val offsetY = (pTileY - centerTileY) * 256.0 + (canvasHeight / 2.0)

                    return Offset(offsetX.toFloat(), offsetY.toFloat())
                }

                // Draw Grid Background lines (representing tile map coordinates)
                val startX = (canvasWidth / 2.0 - centerTileX * 256.0) % 256.0
                val startY = (canvasHeight / 2.0 - centerTileY * 256.0) % 256.0

                var x = startX
                while (x < canvasWidth) {
                    if (x >= 0) {
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            start = Offset(x.toFloat(), 0f),
                            end = Offset(x.toFloat(), canvasHeight),
                            strokeWidth = 1f
                        )
                    }
                    x += 256.0
                }

                var y = startY
                while (y < canvasHeight) {
                    if (y >= 0) {
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            start = Offset(0f, y.toFloat()),
                            end = Offset(canvasWidth, y.toFloat()),
                            strokeWidth = 1f
                        )
                    }
                    y += 256.0
                }

                // Draw Track History Polyline
                if (trackHistory.size >= 2) {
                    val path = Path()
                    val firstOffset = pointToOffset(trackHistory.first().latitude, trackHistory.first().longitude)
                    path.moveTo(firstOffset.x, firstOffset.y)

                    for (i in 1 until trackHistory.size) {
                        val pt = pointToOffset(trackHistory[i].latitude, trackHistory[i].longitude)
                        path.lineTo(pt.x, pt.y)
                    }

                    // Draw Route Shadow
                    drawPath(
                        path = path,
                        color = Color(0xFF003366).copy(alpha = 0.3f),
                        style = Stroke(width = 12f)
                    )

                    // Draw Route Line
                    drawPath(
                        path = path,
                        color = Color(0xFF1976D2),
                        style = Stroke(width = 6f)
                    )

                    // Draw Waypoints
                    trackHistory.forEach { pt ->
                        val off = pointToOffset(pt.latitude, pt.longitude)
                        drawCircle(
                            color = Color.White,
                            radius = 5f,
                            center = off
                        )
                        drawCircle(
                            color = Color(0xFF1976D2),
                            radius = 3f,
                            center = off
                        )
                    }
                }

                // Draw Start Point Marker
                if (trackHistory.isNotEmpty()) {
                    val startPt = trackHistory.first()
                    val startOff = pointToOffset(startPt.latitude, startPt.longitude)
                    drawCircle(color = Color(0xFF388E3C), radius = 10f, center = startOff)
                    drawCircle(color = Color.White, radius = 5f, center = startOff)
                }

                // Draw Current Location Marker
                currentPoint?.let { pt ->
                    val currOff = pointToOffset(pt.latitude, pt.longitude)

                    // Pulsing Outer Ring
                    drawCircle(
                        color = Color(0xFF2196F3).copy(alpha = 0.3f),
                        radius = 24f,
                        center = currOff
                    )
                    // Inner Blue Circle
                    drawCircle(
                        color = Color(0xFF1976D2),
                        radius = 12f,
                        center = currOff
                    )
                    // White Center Dot
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = currOff
                    )
                }
            }
        }

        // Top Dashboard Banner
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.92f),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isTracking) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTracking) "Suivi de trajet actif (Optimisé batterie)" else "Suivi inactif",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isTracking) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                    }

                    Text("Zoom $zoomLevel", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Distance", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = "${(kotlin.math.round(totalDistanceKm * 10.0) / 10.0)} km",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1A3A5A)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Vitesse", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = "${currentPoint?.speedKmh?.toInt() ?: 0} km/h",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1A3A5A)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Points relevés", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = "${trackHistory.size}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1A3A5A)
                        )
                    }
                }
            }
        }

        // Floating Control Bar (Zoom, Center, Start/Stop Tracking, Clear)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Zoom In Button
            SmallFloatingActionButton(
                onClick = { if (zoomLevel < 18) zoomLevel += 1 },
                containerColor = Color.White,
                contentColor = Color(0xFF1A3A5A)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom +")
            }

            // Zoom Out Button
            SmallFloatingActionButton(
                onClick = { if (zoomLevel > 3) zoomLevel -= 1 },
                containerColor = Color.White,
                contentColor = Color(0xFF1A3A5A)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom -")
            }

            // Center on Current Location
            SmallFloatingActionButton(
                onClick = {
                    currentPoint?.let {
                        mapCenterLat = it.latitude
                        mapCenterLon = it.longitude
                    }
                },
                containerColor = Color.White,
                contentColor = Color(0xFF1976D2)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Ma position")
            }

            // Start / Stop Tracking FAB
            FloatingActionButton(
                onClick = {
                    if (isTracking) {
                        locationTracker.stopTracking()
                    } else {
                        locationTracker.startTracking()
                    }
                },
                containerColor = if (isTracking) Color(0xFFD32F2F) else Color(0xFF1B5E20),
                contentColor = Color.White
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isTracking) "Arrêter" else "Démarrer le trajet", fontWeight = FontWeight.Bold)
                }
            }

            // Clear Track FAB
            if (trackHistory.isNotEmpty() && !isTracking) {
                TextButton(
                    onClick = { locationTracker.clearHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Effacer le tracé", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
