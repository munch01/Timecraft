package com.emeric.timecraft

interface Platform {
    val name: String
    fun showToast(message: String)
    fun openUrl(url: String)
    fun openEmail(email: String, subject: String)
    fun exit()
    fun getCurrentLocalDate(): kotlinx.datetime.LocalDate
}

interface SettingsStorage {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun setBoolean(key: String, value: Boolean)
}

expect fun getPlatform(): Platform
expect fun getSettingsStorage(): SettingsStorage
