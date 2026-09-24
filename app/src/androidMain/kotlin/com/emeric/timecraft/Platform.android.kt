package com.emeric.timecraft

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

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

            // 3. RÉPARTITION PAR CLIENT CARD
            if (clientBreakdown.isNotEmpty()) {
                paint.color = android.graphics.Color.rgb(26, 58, 90)
                paint.textSize = 14f
                paint.isFakeBoldText = true
                canvas.drawText("💼 Répartition par Client", cardLeft, currentY + 12f, paint)
                currentY += 22f

                val clientStartY = currentY
                var clientRowY = currentY + 18f
                val clientCardHeight = clientBreakdown.size * rowHeight + 10f

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

                clientBreakdown.forEachIndexed { index, (client, hrs) ->
                    if (index % 2 == 1) {
                        fillPaint.color = android.graphics.Color.rgb(240, 244, 250)
                        canvas.drawRect(
                            cardLeft + 2f, clientRowY - 14f,
                            cardRight - 2f, clientRowY + rowHeight - 14f,
                            fillPaint
                        )
                    }

                    paint.color = android.graphics.Color.rgb(40, 50, 60)
                    paint.textSize = 10.5f
                    paint.isFakeBoldText = true
                    canvas.drawText(client, cardLeft + 12f, clientRowY, paint)

                    paint.color = android.graphics.Color.rgb(26, 58, 90)
                    paint.textSize = 10.5f
                    paint.isFakeBoldText = true
                    val hrsWidth = paint.measureText(hrs)
                    canvas.drawText(hrs, cardRight - 12f - hrsWidth, clientRowY, paint)

                    clientRowY += rowHeight
                }

                currentY = clientStartY + clientCardHeight + 16f
            }

            // 4. MAP VISUALIZATION OF DISPLACEMENTS CARD
            if (trackPoints.size >= 2) {
                paint.color = android.graphics.Color.rgb(26, 58, 90)
                paint.textSize = 14f
                paint.isFakeBoldText = true
                canvas.drawText("🗺️ Carte & Zone de Déplacements GPS", cardLeft, currentY + 12f, paint)
                currentY += 22f

                val mapTopY = currentY
                val mapHeight = 175f
                val mapBottomY = mapTopY + mapHeight

                fillPaint.color = android.graphics.Color.rgb(224, 232, 236) // Terrain background (#E0E8EC)
                val mapRect = android.graphics.RectF(cardLeft, mapTopY, cardRight, mapBottomY)
                canvas.drawRoundRect(mapRect, 10f, 10f, fillPaint)

                // Grid background pattern
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

                // Compute bounding box
                val minLat = trackPoints.minOf { it.latitude }
                val maxLat = trackPoints.maxOf { it.latitude }
                val minLon = trackPoints.minOf { it.longitude }
                val maxLon = trackPoints.maxOf { it.longitude }

                val latSpan = kotlin.math.max(maxLat - minLat, 0.005)
                val lonSpan = kotlin.math.max(maxLon - minLon, 0.005)

                val padMinLat = minLat - latSpan * 0.15
                val padMaxLat = maxLat + latSpan * 0.15
                val padMinLon = minLon - lonSpan * 0.15
                val padMaxLon = maxLon + lonSpan * 0.15

                val innerMargin = 18f
                val drawW = cardWidth - innerMargin * 2
                val drawH = mapHeight - innerMargin * 2

                fun toMapPdfX(lon: Double): Float {
                    return (cardLeft + innerMargin) + ((lon - padMinLon) / (padMaxLon - padMinLon)).toFloat() * drawW
                }

                fun toMapPdfY(lat: Double): Float {
                    return (mapTopY + innerMargin) + ((padMaxLat - lat) / (padMaxLat - padMinLat)).toFloat() * drawH
                }

                // Draw GPS Polyline Path
                val routePath = android.graphics.Path()
                val firstPt = trackPoints.first()
                routePath.moveTo(toMapPdfX(firstPt.longitude), toMapPdfY(firstPt.latitude))

                for (i in 1 until trackPoints.size) {
                    val pt = trackPoints[i]
                    routePath.lineTo(toMapPdfX(pt.longitude), toMapPdfY(pt.latitude))
                }

                // Polyline casing / shadow
                strokePaint.color = android.graphics.Color.WHITE
                strokePaint.strokeWidth = 5.5f
                canvas.drawPath(routePath, strokePaint)

                // Polyline main track
                strokePaint.color = android.graphics.Color.rgb(25, 118, 210) // Blue #1976D2
                strokePaint.strokeWidth = 3f
                canvas.drawPath(routePath, strokePaint)

                // Waypoints
                fillPaint.color = android.graphics.Color.rgb(25, 118, 210)
                trackPoints.forEach { pt ->
                    canvas.drawCircle(toMapPdfX(pt.longitude), toMapPdfY(pt.latitude), 2.5f, fillPaint)
                }

                // Start Marker (Green Circle)
                val startX = toMapPdfX(firstPt.longitude)
                val startY = toMapPdfY(firstPt.latitude)
                fillPaint.color = android.graphics.Color.rgb(46, 125, 50)
                canvas.drawCircle(startX, startY, 6.5f, fillPaint)
                fillPaint.color = android.graphics.Color.WHITE
                canvas.drawCircle(startX, startY, 2.5f, fillPaint)

                // End Marker (Red Circle)
                val lastPt = trackPoints.last()
                val endX = toMapPdfX(lastPt.longitude)
                val endY = toMapPdfY(lastPt.latitude)
                fillPaint.color = android.graphics.Color.rgb(198, 40, 40)
                canvas.drawCircle(endX, endY, 6.5f, fillPaint)
                fillPaint.color = android.graphics.Color.WHITE
                canvas.drawCircle(endX, endY, 2.5f, fillPaint)

                // Map Container Border
                strokePaint.color = android.graphics.Color.rgb(26, 58, 90)
                strokePaint.strokeWidth = 1.2f
                canvas.drawRoundRect(mapRect, 10f, 10f, strokePaint)

                // Map Info Badge (Pill in Top Right of Map)
                val badgeText = "📍 Zone GPS • ${trackPoints.size} points"
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
