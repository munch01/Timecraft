package com.example.timecraft

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.timecraft.network.supabase
import com.example.timecraft.screens.*
import com.example.timecraft.ui.theme.TimeCraftTheme
import com.example.timecraft.viewmodel.CalendarViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.datetime.LocalDate

enum class Screen {
    Auth, Home, DayDetail, Settings
}

@Composable
fun App() {
    TimeCraftTheme {
        var currentScreen by remember { mutableStateOf(if (supabase.auth.currentSessionOrNull() != null) Screen.Home else Screen.Auth) }
        var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
        val calendarViewModel = remember { CalendarViewModel() }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFE8ECEF)
        ) {
            when (currentScreen) {
                Screen.Auth -> AuthScreen(onAuthSuccess = { currentScreen = Screen.Home })
                Screen.Home -> HomeScreen(
                    calendarViewModel = calendarViewModel,
                    onNavigateToSettings = { currentScreen = Screen.Settings },
                    onNavigateToReport = { /* Integrated in Home tabs */ },
                    onDayClick = { date ->
                        selectedDate = date
                        currentScreen = Screen.DayDetail
                    }
                )
                Screen.DayDetail -> selectedDate?.let { date ->
                    DayDetailScreen(
                        date = date,
                        onBack = { currentScreen = Screen.Home },
                        onSave = { _, _, _ ->
                            currentScreen = Screen.Home
                        }
                    )
                }
                Screen.Settings -> SettingsScreen(
                    onBackClick = { currentScreen = Screen.Home },
                    onLogout = { currentScreen = Screen.Auth }
                )
            }
        }
    }
}
