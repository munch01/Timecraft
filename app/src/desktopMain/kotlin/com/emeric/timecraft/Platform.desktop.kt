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

actual fun getPlatform(): Platform = DesktopPlatform()
actual fun getSettingsStorage(): SettingsStorage = DesktopSettingsStorage()
