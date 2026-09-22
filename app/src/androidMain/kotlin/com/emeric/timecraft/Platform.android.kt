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
        clientBreakdown: List<Pair<String, String>>
    ) {
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = android.graphics.Paint()

            paint.color = android.graphics.Color.rgb(26, 58, 90)
            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText(title, 40f, 50f, paint)

            paint.color = android.graphics.Color.GRAY
            paint.textSize = 12f
            paint.isFakeBoldText = false
            canvas.drawText(subtitle, 40f, 72f, paint)

            paint.color = android.graphics.Color.LTGRAY
            canvas.drawLine(40f, 85f, 555f, 85f, paint)

            var y = 115f
            paint.color = android.graphics.Color.rgb(26, 58, 90)
            paint.textSize = 15f
            paint.isFakeBoldText = true
            canvas.drawText("Synthèse de la période", 40f, y, paint)
            y += 24f

            paint.textSize = 11f
            metrics.forEach { (label, value) ->
                paint.color = android.graphics.Color.DKGRAY
                paint.isFakeBoldText = false
                canvas.drawText(label, 50f, y, paint)

                paint.color = android.graphics.Color.BLACK
                paint.isFakeBoldText = true
                canvas.drawText(value, 380f, y, paint)
                y += 20f
            }

            if (clientBreakdown.isNotEmpty()) {
                y += 15f
                paint.color = android.graphics.Color.rgb(26, 58, 90)
                paint.textSize = 15f
                paint.isFakeBoldText = true
                canvas.drawText("Répartition par Client", 40f, y, paint)
                y += 24f

                clientBreakdown.forEach { (client, hrs) ->
                    paint.color = android.graphics.Color.DKGRAY
                    paint.isFakeBoldText = false
                    canvas.drawText(client, 50f, y, paint)

                    paint.color = android.graphics.Color.BLACK
                    paint.isFakeBoldText = true
                    canvas.drawText(hrs, 380f, y, paint)
                    y += 20f
                }
            }

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
