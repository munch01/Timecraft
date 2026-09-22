package com.emeric.timecraft

interface Platform {
    val name: String
    fun showToast(message: String)
    fun openUrl(url: String)
    fun openEmail(email: String, subject: String)
    fun openAppSettings()
    fun exit()
    fun getCurrentLocalDate(): kotlinx.datetime.LocalDate
}

interface SettingsStorage {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun setBoolean(key: String, value: Boolean)
    fun getString(key: String, defaultValue: String): String
    fun setString(key: String, value: String)
}

interface PdfExporter {
    fun exportReportPdf(
        title: String,
        subtitle: String,
        metrics: List<Pair<String, String>>,
        clientBreakdown: List<Pair<String, String>>
    )
}

expect fun getPlatform(): Platform
expect fun getSettingsStorage(): SettingsStorage
expect fun getPdfExporter(): PdfExporter
