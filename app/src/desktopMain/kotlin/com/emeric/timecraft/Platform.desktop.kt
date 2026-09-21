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

    override fun exit() {
        System.exit(0)
    }

    override fun getCurrentLocalDate(): kotlinx.datetime.LocalDate {
        val now = java.time.LocalDate.now()
        return kotlinx.datetime.LocalDate(now.year, now.monthValue, now.dayOfMonth)
    }
}

actual fun getPlatform(): Platform = DesktopPlatform()
