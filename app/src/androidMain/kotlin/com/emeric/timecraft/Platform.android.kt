package com.emeric.timecraft

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

private fun lonToWorldX(lon: Double, zoom: Int): Double {
    val n = 1 shl zoom
    return (lon + 180.0) / 360.0 * n * 256.0
}

private fun latToWorldY(lat: Double, zoom: Int): Double {
    val rad = lat * kotlin.math.PI / 180.0
    val n = 1 shl zoom
    return (1.0 - kotlin.math.ln(kotlin.math.tan(rad) + 1.0 / kotlin.math.cos(rad)) / kotlin.math.PI) / 2.0 * n * 256.0
}

private fun fetchMapTile(context: Context, zoom: Int, x: Int, y: Int): android.graphics.Bitmap? {
    val cacheDir = java.io.File(context.cacheDir, "map_tiles").apply { mkdirs() }
    val tileFile = java.io.File(cacheDir, "tile_${zoom}_${x}_${y}.png")
    if (tileFile.exists() && tileFile.length() > 0) {
        try {
            return android.graphics.BitmapFactory.decodeFile(tileFile.absolutePath)
        } catch (e: Exception) {
            tileFile.delete()
        }
    }

    val urls = listOf(
        "https://a.basemaps.cartocdn.com/light_all/$zoom/$x/$y.png",
        "https://b.basemaps.cartocdn.com/light_all/$zoom/$x/$y.png",
        "https://tile.openstreetmap.org/$zoom/$x/$y.png"
    )

    for (urlStr in urls) {
        try {
            val url = java.net.URL(urlStr)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 3500
            conn.readTimeout = 3500
            conn.setRequestProperty("User-Agent", "TimeCraft-Android/1.0 (Contact: emeric@timecraft.app)")
            if (conn.responseCode == 200) {
                conn.inputStream.use { input ->
                    tileFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return android.graphics.BitmapFactory.decodeFile(tileFile.absolutePath)
            }
        } catch (e: Exception) {
            // continue
        }
    }
    return null
}

class AndroidPlatform(private val context: Context) : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
    
    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun openUrl(url: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun openEmail(email: String, subject: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:$email")
            putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Erreur : aucune appli d'email")
        }
    }

    override fun openAppSettings() {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showToast("Impossible d'ouvrir les paramètres")
        }
    }

    override fun exit() {
        currentActivity?.finish()
    }

    override fun getCurrentLocalDate(): kotlinx.datetime.LocalDate {
        val cal = java.util.Calendar.getInstance()
        return kotlinx.datetime.LocalDate(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }
}

class AndroidSettingsStorage(private val context: Context) : SettingsStorage {
    private val prefs = context.getSharedPreferences("timecraft_prefs", Context.MODE_PRIVATE)
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = prefs.getBoolean(key, defaultValue)
    override fun setBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    override fun getString(key: String, defaultValue: String): String = prefs.getString(key, defaultValue) ?: defaultValue
    override fun setString(key: String, value: String) = prefs.edit().putString(key, value).apply()
}

class AndroidPdfExporter(private val context: Context) : PdfExporter {
    override fun exportReportPdf(
        title: String,
        subtitle: String,
        metrics: List<Pair<String, String>>,
        clientBreakdown: List<Pair<String, String>>,
        trackPoints: List<LocationPoint>
    ) {
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { style = android.graphics.Paint.Style.FILL }
            val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { style = android.graphics.Paint.Style.STROKE }

            // 1. HEADER BANNER
            val headerHeight = 85f
            fillPaint.color = android.graphics.Color.rgb(26, 58, 90) // Navy #1A3A5A
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), headerHeight, fillPaint)

            paint.color = android.graphics.Color.WHITE
            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText(title, 30f, 38f, paint)

            paint.color = android.graphics.Color.rgb(200, 220, 240)
            paint.textSize = 11f
            paint.isFakeBoldText = false
            canvas.drawText(subtitle, 30f, 60f, paint)

            paint.color = android.graphics.Color.WHITE
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("TIMECRAFT", pageWidth - 110f, 38f, paint)

            var currentY = headerHeight + 18f
            val cardLeft = 25f
            val cardRight = pageWidth - 25f
            val cardWidth = cardRight - cardLeft

            // 2. SYNTHÈSE DE LA PÉRIODE CARD
            paint.color = android.graphics.Color.rgb(26, 58, 90)
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText("📊 Synthèse de la période", cardLeft, currentY + 12f, paint)
            currentY += 22f

            val metricsStartY = currentY
            var metricRowY = currentY + 18f
            val rowHeight = 24f
            val metricsCardHeight = metrics.size * rowHeight + 10f

            fillPaint.color = android.graphics.Color.rgb(248, 250, 252)
            canvas.drawRoundRect(
                android.graphics.RectF(cardLeft, metricsStartY, cardRight, metricsStartY + metricsCardHeight),
                10f, 10f, fillPaint
            )

            strokePaint.color = android.graphics.Color.rgb(220, 230, 240)
            strokePaint.strokeWidth = 1f
            canvas.drawRoundRect(
                android.graphics.RectF(cardLeft, metricsStartY, cardRight, metricsStartY + metricsCardHeight),
                10f, 10f, strokePaint
            )

            metrics.forEachIndexed { index, (label, value) ->
                if (index % 2 == 1) {
                    fillPaint.color = android.graphics.Color.rgb(240, 244, 250)
                    canvas.drawRect(
                        cardLeft + 2f, metricRowY - 14f,
                        cardRight - 2f, metricRowY + rowHeight - 14f,
                        fillPaint
                    )
                }

                paint.color = android.graphics.Color.rgb(40, 50, 60)
                paint.textSize = 10.5f
                paint.isFakeBoldText = false
                canvas.drawText(label, cardLeft + 12f, metricRowY, paint)

                paint.color = android.graphics.Color.rgb(26, 58, 90)
                paint.textSize = 10.5f
                paint.isFakeBoldText = true
                val valWidth = paint.measureText(value)
                canvas.drawText(value, cardRight - 12f - valWidth, metricRowY, paint)

                metricRowY += rowHeight
            }

            currentY = metricsStartY + metricsCardHeight + 16f

            // 3. RÉPARTITION PAR CLIENT CARD (2 COLONNES)
            if (clientBreakdown.isNotEmpty()) {
                paint.color = android.graphics.Color.rgb(26, 58, 90)
                paint.textSize = 13.5f
                paint.isFakeBoldText = true
                canvas.drawText("💼 Répartition par Client", cardLeft, currentY + 12f, paint)
                currentY += 20f

                val clientStartY = currentY
                val numRows = (clientBreakdown.size + 1) / 2
                val clientRowHeight = 22f
                val clientCardHeight = numRows * clientRowHeight + 10f

                fillPaint.color = android.graphics.Color.rgb(248, 250, 252)
                canvas.drawRoundRect(
                    android.graphics.RectF(cardLeft, clientStartY, cardRight, clientStartY + clientCardHeight),
                    10f, 10f, fillPaint
                )

                strokePaint.color = android.graphics.Color.rgb(220, 230, 240)
                strokePaint.strokeWidth = 1f
                canvas.drawRoundRect(
                    android.graphics.RectF(cardLeft, clientStartY, cardRight, clientStartY + clientCardHeight),
                    10f, 10f, strokePaint
                )

                val colWidth = (cardWidth - 24f) / 2f

                clientBreakdown.forEachIndexed { index, (client, hrs) ->
                    val col = index % 2
                    val row = index / 2
                    val itemX = if (col == 0) cardLeft + 12f else cardLeft + colWidth + 20f
                    val itemRowY = clientStartY + 16f + (row * clientRowHeight)

                    if (col == 1) {
                        strokePaint.color = android.graphics.Color.rgb(230, 235, 245)
                        canvas.drawLine(cardLeft + colWidth + 10f, clientStartY + 4f, cardLeft + colWidth + 10f, clientStartY + clientCardHeight - 4f, strokePaint)
                    }

                    paint.color = android.graphics.Color.rgb(40, 50, 60)
                    paint.textSize = 10f
                    paint.isFakeBoldText = true
                    canvas.drawText(client, itemX, itemRowY, paint)

                    paint.color = android.graphics.Color.rgb(26, 58, 90)
                    paint.textSize = 10f
                    paint.isFakeBoldText = true
                    val hrsWidth = paint.measureText(hrs)
                    canvas.drawText(hrs, itemX + colWidth - 12f - hrsWidth, itemRowY, paint)
                }

                currentY = clientStartY + clientCardHeight + 14f
            }

            // 4. CARTE DES DÉPLACEMENTS (FOND DE CARTE RÉEL OPENSTREETMAP / CARTO)
            if (trackPoints.size >= 2) {
                paint.color = android.graphics.Color.rgb(26, 58, 90)
                paint.textSize = 13.5f
                paint.isFakeBoldText = true
                canvas.drawText("🗺️ Carte & Zone de Déplacements GPS", cardLeft, currentY + 12f, paint)
                currentY += 20f

                val mapTopY = currentY
                val mapHeight = 240f
                val mapBottomY = mapTopY + mapHeight

                val mapRect = android.graphics.RectF(cardLeft, mapTopY, cardRight, mapBottomY)

                canvas.save()
                val clipPath = android.graphics.Path()
                clipPath.addRoundRect(mapRect, 10f, 10f, android.graphics.Path.Direction.CW)
                canvas.clipPath(clipPath)

                // Base Map Terrain fill
                fillPaint.color = android.graphics.Color.rgb(238, 242, 245)
                canvas.drawRect(mapRect, fillPaint)

                // Compute bounding box
                val minLat = trackPoints.minOf { it.latitude }
                val maxLat = trackPoints.maxOf { it.latitude }
                val minLon = trackPoints.minOf { it.longitude }
                val maxLon = trackPoints.maxOf { it.longitude }

                val latSpanDeg = kotlin.math.max(maxLat - minLat, 0.002)
                val lonSpanDeg = kotlin.math.max(maxLon - minLon, 0.002)

                val padMinLat = minLat - latSpanDeg * 0.12
                val padMaxLat = maxLat + latSpanDeg * 0.12
                val padMinLon = minLon - lonSpanDeg * 0.12
                val padMaxLon = maxLon + lonSpanDeg * 0.12

                // Calculate zoom level
                var zoom = 16
                while (zoom > 2) {
                    val wX1 = lonToWorldX(padMinLon, zoom)
                    val wX2 = lonToWorldX(padMaxLon, zoom)
                    val wY1 = latToWorldY(padMaxLat, zoom)
                    val wY2 = latToWorldY(padMinLat, zoom)
                    val sX = kotlin.math.abs(wX2 - wX1)
                    val sY = kotlin.math.abs(wY2 - wY1)
                    if (sX <= cardWidth.toDouble() * 0.85 && sY <= mapHeight.toDouble() * 0.85) {
                        break
                    }
                    zoom--
                }

                val centerWX = (lonToWorldX(padMinLon, zoom) + lonToWorldX(padMaxLon, zoom)) / 2.0
                val centerWY = (latToWorldY(padMinLat, zoom) + latToWorldY(padMaxLat, zoom)) / 2.0

                val rawSpanX = kotlin.math.abs(lonToWorldX(padMaxLon, zoom) - lonToWorldX(padMinLon, zoom))
                val rawSpanY = kotlin.math.abs(latToWorldY(padMinLat, zoom) - latToWorldY(padMaxLat, zoom))

                val scale = kotlin.math.min(cardWidth / kotlin.math.max(rawSpanX, 1.0), mapHeight / kotlin.math.max(rawSpanY, 1.0))

                val centerX = cardLeft + cardWidth / 2f
                val centerY = mapTopY + mapHeight / 2f

                fun toMapPdfX(lon: Double): Float {
                    return (centerX + (lonToWorldX(lon, zoom) - centerWX) * scale).toFloat()
                }

                fun toMapPdfY(lat: Double): Float {
                    return (centerY + (latToWorldY(lat, zoom) - centerWY) * scale).toFloat()
                }

                // Render Map Tiles
                val minWX = centerWX - (cardWidth / 2f) / scale
                val maxWX = centerWX + (cardWidth / 2f) / scale
                val minWY = centerWY - (mapHeight / 2f) / scale
                val maxWY = centerWY + (mapHeight / 2f) / scale

                val minTileX = kotlin.math.floor(minWX / 256.0).toInt()
                val maxTileX = kotlin.math.floor(maxWX / 256.0).toInt()
                val minTileY = kotlin.math.floor(minWY / 256.0).toInt()
                val maxTileY = kotlin.math.floor(maxWY / 256.0).toInt()

                var tilesRendered = 0
                for (tx in minTileX..maxTileX) {
                    for (ty in minTileY..maxTileY) {
                        val tileLeft = centerX + (tx * 256.0 - centerWX) * scale
                        val tileTop = centerY + (ty * 256.0 - centerWY) * scale
                        val tileRight = tileLeft + 256.0 * scale
                        val tileBottom = tileTop + 256.0 * scale

                        val destRect = android.graphics.RectF(tileLeft.toFloat(), tileTop.toFloat(), tileRight.toFloat(), tileBottom.toFloat())
                        val tileBmp = fetchMapTile(context, zoom, tx, ty)
                        if (tileBmp != null) {
                            canvas.drawBitmap(tileBmp, null as android.graphics.Rect?, destRect, paint)
                            tilesRendered++
                        }
                    }
                }

                // Vector grid fallback if offline
                if (tilesRendered == 0) {
                    strokePaint.color = android.graphics.Color.rgb(205, 215, 222)
                    strokePaint.strokeWidth = 1f
                    var gx = cardLeft + 30f
                    while (gx < cardRight) {
                        canvas.drawLine(gx, mapTopY, gx, mapBottomY, strokePaint)
                        gx += 40f
                    }
                    var gy = mapTopY + 30f
                    while (gy < mapBottomY) {
                        canvas.drawLine(cardLeft, gy, cardRight, gy, strokePaint)
                        gy += 40f
                    }
                }

                // Draw GPS Polyline Path
                val routePath = android.graphics.Path()
                val firstPt = trackPoints.first()
                routePath.moveTo(toMapPdfX(firstPt.longitude), toMapPdfY(firstPt.latitude))

                for (i in 1 until trackPoints.size) {
                    val pt = trackPoints[i]
                    routePath.lineTo(toMapPdfX(pt.longitude), toMapPdfY(pt.latitude))
                }

                // Polyline casing
                strokePaint.color = android.graphics.Color.WHITE
                strokePaint.strokeWidth = 7f
                canvas.drawPath(routePath, strokePaint)

                // Polyline main track
                strokePaint.color = android.graphics.Color.rgb(21, 101, 192) // Vibrant Blue #1565C0
                strokePaint.strokeWidth = 4f
                canvas.drawPath(routePath, strokePaint)

                // Waypoints
                fillPaint.color = android.graphics.Color.rgb(21, 101, 192)
                trackPoints.forEach { pt ->
                    canvas.drawCircle(toMapPdfX(pt.longitude), toMapPdfY(pt.latitude), 2.5f, fillPaint)
                }

                // Start Marker (Green Circle)
                val startX = toMapPdfX(firstPt.longitude)
                val startY = toMapPdfY(firstPt.latitude)
                fillPaint.color = android.graphics.Color.rgb(46, 125, 50)
                canvas.drawCircle(startX, startY, 7f, fillPaint)
                fillPaint.color = android.graphics.Color.WHITE
                canvas.drawCircle(startX, startY, 3f, fillPaint)

                // End Marker (Red Circle)
                val lastPt = trackPoints.last()
                val endX = toMapPdfX(lastPt.longitude)
                val endY = toMapPdfY(lastPt.latitude)
                fillPaint.color = android.graphics.Color.rgb(198, 40, 40)
                canvas.drawCircle(endX, endY, 7f, fillPaint)
                fillPaint.color = android.graphics.Color.WHITE
                canvas.drawCircle(endX, endY, 3f, fillPaint)

                canvas.restore()

                // Map Container Border
                strokePaint.color = android.graphics.Color.rgb(26, 58, 90)
                strokePaint.strokeWidth = 1.2f
                canvas.drawRoundRect(mapRect, 10f, 10f, strokePaint)

                // Map Info Badge (Pill in Top Right of Map)
                val badgeText = if (tilesRendered > 0) "🗺️ Carte OSM • ${trackPoints.size} pts" else "📍 Zone GPS • ${trackPoints.size} pts"
                paint.textSize = 9.5f
                paint.isFakeBoldText = true
                val badgeTextW = paint.measureText(badgeText)
                val badgeW = badgeTextW + 14f
                val badgeH = 18f
                val badgeRect = android.graphics.RectF(
                    cardRight - badgeW - 8f,
                    mapTopY + 8f,
                    cardRight - 8f,
                    mapTopY + 8f + badgeH
                )

                fillPaint.color = android.graphics.Color.argb(230, 255, 255, 255)
                canvas.drawRoundRect(badgeRect, 8f, 8f, fillPaint)

                paint.color = android.graphics.Color.rgb(26, 58, 90)
                canvas.drawText(badgeText, cardRight - badgeW, mapTopY + 21f, paint)

                currentY = mapBottomY + 16f
            }

            // FOOTER
            paint.color = android.graphics.Color.GRAY
            paint.textSize = 8.5f
            paint.isFakeBoldText = false
            canvas.drawText("Généré avec TimeCraft le ${java.time.LocalDate.now()}", 30f, pageHeight - 18f, paint)
            val pageStr = "Page 1/1"
            canvas.drawText(pageStr, pageWidth - 30f - paint.measureText(pageStr), pageHeight - 18f, paint)

            pdfDocument.finishPage(page)

            val file = java.io.File(context.cacheDir, "Rapport_TimeCraft_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(java.io.FileOutputStream(file))
            pdfDocument.close()

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            getPlatform().showToast("PDF généré et ouvert")
        } catch (e: Exception) {
            e.printStackTrace()
            getPlatform().showToast("Erreur PDF : ${e.message}")
        }
    }
}

lateinit var appContext: Context
var currentActivity: androidx.fragment.app.FragmentActivity? = null

actual fun getPlatform(): Platform = AndroidPlatform(appContext)
actual fun getSettingsStorage(): SettingsStorage = AndroidSettingsStorage(appContext)
actual fun getPdfExporter(): PdfExporter = AndroidPdfExporter(appContext)
