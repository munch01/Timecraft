package com.example.timecraft

interface Platform {
    val name: String
    fun showToast(message: String)
    fun openUrl(url: String)
    fun openEmail(email: String, subject: String)
    fun exit()
    fun getCurrentLocalDate(): kotlinx.datetime.LocalDate
}

expect fun getPlatform(): Platform
