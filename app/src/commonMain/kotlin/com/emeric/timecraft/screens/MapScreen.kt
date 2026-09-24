package com.emeric.timecraft.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emeric.timecraft.MapProjection
import com.emeric.timecraft.getLocationTracker
import com.emeric.timecraft.getPlatform
import com.emeric.timecraft.getSettingsStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap

class TileCache {
    private val memoryCache = mutableMapOf<String, ImageBitmap>()

    suspend fun getTile(zoom: Int, tileX: Int, tileY: Int): ImageBitmap? = withContext(Dispatchers.Default) {
        val key = "$zoom/$tileX/$tileY"
        memoryCache[key]?.let { return@withContext it }

        val tileUrls = listOf(
            "https://tile.openstreetmap.org/$zoom/$tileX/$tileY.png",
            "https://a.tile.openstreetmap.fr/osmfr/$zoom/$tileX/$tileY.png",
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/$zoom/$tileY/$tileX"
        )

        for (urlStr in tileUrls) {
            try {
                val url = java.net.URL(urlStr)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "TimeCraft/1.0 (Android; Mobile)")
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.instanceFollowRedirects = true
                conn.connect()

                if (conn.responseCode == 200) {
                    val bytes = conn.inputStream.use { it.readBytes() }
                    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
                    val bitmap = bytes.decodeToImageBitmap()
                    memoryCache[key] = bitmap
                    return@withContext bitmap
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        null
    }
}

@Composable
fun MapScreen(
    modifier: Modifier = Modifier
) {
    val locationTracker = remember { getLocationTracker() }
    val platform = remember { getPlatform() }
    val settingsStorage = remember { getSettingsStorage() }
    val tileCache = remember { TileCache() }
    val coroutineScope = rememberCoroutineScope()

    val isTracking by locationTracker.isTracking.collectAsState()
    val currentPoint by locationTracker.currentPoint.collectAsState()
    val trackHistory by locationTracker.trackHistory.collectAsState()

    var mapCenterLat by remember { mutableStateOf(48.8566) } // Paris default
    var mapCenterLon by remember { mutableStateOf(2.3522) }
    var zoomLevel by remember { mutableStateOf(14) } // Zoom 3 to 18

    var showPermissionDialog by remember { mutableStateOf(false) }
    var hasAskedPermission by remember { mutableStateOf(settingsStorage.getBoolean("gps_permission_asked", false)) }

    val activeTiles = remember { mutableStateMapOf<String, ImageBitmap>() }

    // Auto-center map on current position when available
    LaunchedEffect(currentPoint) {
        currentPoint?.let {
            if (trackHistory.size <= 1) {
                mapCenterLat = it.latitude
                mapCenterLon = it.longitude
            }
        }
    }

    // Load tiles around map center
    LaunchedEffect(mapCenterLat, mapCenterLon, zoomLevel) {
        val centerTileX = MapProjection.lonToTileX(mapCenterLon, zoomLevel).toInt()
        val centerTileY = MapProjection.latToTileY(mapCenterLat, zoomLevel).toInt()

        for (dx in -2..2) {
            for (dy in -2..2) {
                val tx = centerTileX + dx
                val ty = centerTileY + dy
                val key = "$zoomLevel/$tx/$ty"
                if (!activeTiles.containsKey(key)) {
                    coroutineScope.launch {
                        val bmp = tileCache.getTile(zoomLevel, tx, ty)
                        if (bmp != null) {
                            activeTiles[key] = bmp
                        }
                    }
                }
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
            .background(Color(0xFFE0E8EC))
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

                // Draw Map Tiles
                val baseTileX = centerTileX.toInt()
                val baseTileY = centerTileY.toInt()

                for (dx in -3..3) {
                    for (dy in -3..3) {
                        val tx = baseTileX + dx
                        val ty = baseTileY + dy
                        val key = "$zoomLevel/$tx/$ty"
                        val bmp = activeTiles[key]

                        val tileOffsetX = (tx - centerTileX) * 256.0 + (canvasWidth / 2.0)
                        val tileOffsetY = (ty - centerTileY) * 256.0 + (canvasHeight / 2.0)

                        if (bmp != null) {
                            drawImage(
                                image = bmp,
                                dstOffset = IntOffset(tileOffsetX.toInt(), tileOffsetY.toInt())
                            )
                        } else {
                            // Grid placeholder
                            drawRect(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                topLeft = Offset(tileOffsetX.toFloat(), tileOffsetY.toFloat()),
                                size = androidx.compose.ui.geometry.Size(256f, 256f)
                            )
                        }
                    }
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

                    // Route Shadow
                    drawPath(
                        path = path,
                        color = Color(0xFF003366).copy(alpha = 0.3f),
                        style = Stroke(width = 12f)
                    )

                    // Route Main Line
                    drawPath(
                        path = path,
                        color = Color(0xFF1976D2),
                        style = Stroke(width = 6f)
                    )

                    // Waypoints
                    trackHistory.forEach { pt ->
                        val off = pointToOffset(pt.latitude, pt.longitude)
                        drawCircle(color = Color.White, radius = 5f, center = off)
                        drawCircle(color = Color(0xFF1976D2), radius = 3f, center = off)
                    }
                }

                // Draw Start Marker
                if (trackHistory.isNotEmpty()) {
                    val startPt = trackHistory.first()
                    val startOff = pointToOffset(startPt.latitude, startPt.longitude)
                    drawCircle(color = Color(0xFF388E3C), radius = 10f, center = startOff)
                    drawCircle(color = Color.White, radius = 5f, center = startOff)
                }

                // Draw Current Location Marker
                currentPoint?.let { pt ->
                    val currOff = pointToOffset(pt.latitude, pt.longitude)

                    // Pulsing Ring
                    drawCircle(
                        color = Color(0xFF2196F3).copy(alpha = 0.35f),
                        radius = 24f,
                        center = currOff
                    )
                    // Inner Circle
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
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            text = if (isTracking) "Suivi GPS actif" else "Suivi inactif",
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
                        Text("Distance parcourue", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = "${(kotlin.math.round(totalDistanceKm * 10.0) / 10.0)} km",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1A3A5A)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Points enregistrés", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = "${trackHistory.size}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1A3A5A)
                        )
                    }
                }
            }
        }

        // Floating Control Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            SmallFloatingActionButton(
                onClick = { if (zoomLevel < 18) zoomLevel += 1 },
                containerColor = Color.White,
                contentColor = Color(0xFF1A3A5A)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom +")
            }

            SmallFloatingActionButton(
                onClick = { if (zoomLevel > 3) zoomLevel -= 1 },
                containerColor = Color.White,
                contentColor = Color(0xFF1A3A5A)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom -")
            }

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

            FloatingActionButton(
                onClick = {
                    if (!hasAskedPermission) {
                        showPermissionDialog = true
                    } else if (isTracking) {
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

    // Permission Explanation Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF1A3A5A), modifier = Modifier.size(36.dp)) },
            title = { Text("Autoriser la Géolocalisation", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TimeCraft a besoin d'accéder à votre position GPS pour enregistrer vos trajets professionnels et calculer vos kilomètres parcourus.")
                    Text("Pour permettre le suivi en arrière-plan (téléphone verrouillé ou application fermée), veuillez autoriser la localisation dans les paramètres de votre téléphone.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                    Button(
                        onClick = {
                            settingsStorage.setBoolean("gps_permission_asked", true)
                            hasAskedPermission = true
                            showPermissionDialog = false
                            platform.openAppSettings()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A5A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Paramètres du téléphone")
                    }

                    OutlinedButton(
                        onClick = {
                            settingsStorage.setBoolean("gps_permission_asked", true)
                            hasAskedPermission = true
                            showPermissionDialog = false
                            locationTracker.startTracking()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Démarrer directement")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
