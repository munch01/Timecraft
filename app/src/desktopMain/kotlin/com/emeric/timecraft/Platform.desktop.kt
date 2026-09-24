package com.emeric.timecraft

class DesktopPlatform : Platform {
    override val name: String = "Desktop"
    
    override fun showToast(message: String) {
        println("TOAST: $message")
    }

    override fun openUrl(url: String) {
        java.awt.Desktop.getDesktop().browse(java.net.URI(url))
    }

    override fun openEmail(email: String, subject: String) {
        java.awt.Desktop.getDesktop().mail(java.net.URI("mailto:$email?subject=${subject.replace(" ", "%20")}"))
    }

    override fun openAppSettings() {
        showToast("Paramètres non requis sur Desktop")
    }

    override fun exit() {
        System.exit(0)
    }

    override fun getCurrentLocalDate(): kotlinx.datetime.LocalDate {
        val now = java.time.LocalDate.now()
        return kotlinx.datetime.LocalDate(now.year, now.monthValue, now.dayOfMonth)
    }
}

class DesktopSettingsStorage : SettingsStorage {
    private val prefs = java.util.prefs.Preferences.userNodeForPackage(DesktopSettingsStorage::class.java)
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = prefs.getBoolean(key, defaultValue)
    override fun setBoolean(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
    }
    override fun getString(key: String, defaultValue: String): String = prefs.get(key, defaultValue)
    override fun setString(key: String, value: String) {
        prefs.put(key, value)
    }
}

class DesktopPdfExporter : PdfExporter {
    override fun exportReportPdf(
        title: String,
        subtitle: String,
        metrics: List<Pair<String, String>>,
        clientBreakdown: List<Pair<String, String>>,
        trackPoints: List<LocationPoint>
    ) {
        try {
            val userHome = System.getProperty("user.home") ?: "."
            val file = java.io.File(userHome, "Rapport_TimeCraft_${System.currentTimeMillis()}.html")
            val htmlContent = buildString {
                append("<html><head><meta charset='UTF-8'><style>")
                append("body { font-family: Arial, sans-serif; margin: 40px; color: #1A3A5A; }")
                append("h1 { color: #1A3A5A; } table { width: 100%; border-collapse: collapse; margin-top: 20px; }")
                append("th, td { padding: 10px; border-bottom: 1px solid #ddd; text-align: left; }")
                append("th { background-color: #1A3A5A; color: white; }")
                append("</style></head><body>")
                append("<h1>$title</h1><p>$subtitle</p><hr>")
                append("<h2>Synthèse</h2><table><tr><th>Indicateur</th><th>Valeur</th></tr>")
                metrics.forEach { (m, v) ->
                    append("<tr><td>$m</td><td><b>$v</b></td></tr>")
                }
                append("</table>")
                if (clientBreakdown.isNotEmpty()) {
                    append("<h2>Répartition par Client</h2><table><tr><th>Client</th><th>Heures</th></tr>")
                    clientBreakdown.forEach { (c, h) ->
                        append("<tr><td>$c</td><td><b>$h</b></td></tr>")
                    }
                    append("</table>")
                }
                append("</body></html>")
            }
            file.writeText(htmlContent)
            java.awt.Desktop.getDesktop().browse(file.toURI())
            getPlatform().showToast("Rapport HTML généré : ${file.name}")
        } catch (e: Exception) {
            e.printStackTrace()
            getPlatform().showToast("Erreur export : ${e.message}")
        }
    }
}

actual fun getPlatform(): Platform = DesktopPlatform()
actual fun getSettingsStorage(): SettingsStorage = DesktopSettingsStorage()
actual fun getPdfExporter(): PdfExporter = DesktopPdfExporter()
