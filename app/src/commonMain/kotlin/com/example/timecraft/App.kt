package com.example.timecraft

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.timecraft.network.supabase
import com.example.timecraft.screens.*
import com.example.timecraft.ui.theme.TimeCraftTheme
import com.example.timecraft.viewmodel.CalendarViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import timecraft.app.generated.resources.Res
import timecraft.app.generated.resources.*

enum class Screen {
    Splash, Auth, Home, DayDetail, Settings
}

@Composable
fun App() {
    TimeCraftTheme {
        var currentScreen by remember { mutableStateOf(Screen.Splash) }
        var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
        val calendarViewModel = remember { CalendarViewModel() }

        // Splash screen logic
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000) // 2 seconds splash
            currentScreen = try {
                if (supabase.auth.currentSessionOrNull() != null) Screen.Home else Screen.Auth
            } catch (e: Exception) {
                Screen.Auth
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFE8ECEF)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Persistent background for all screens (except maybe splash)
                if (currentScreen != Screen.Splash) {
                    Image(
                        painter = painterResource(Res.drawable.app_bg),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.3f
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.logo_minimal),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(0.9f),
                            contentScale = ContentScale.Fit,
                            alpha = 0.15f
                        )
                    }
                }

                when (currentScreen) {
                    Screen.Splash -> SplashScreen()
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
                            initialWorkDay = calendarViewModel.workDays[date],
                            onBack = { currentScreen = Screen.Home },
                            onSave = { updatedWorkDay ->
                                calendarViewModel.saveWorkDay(updatedWorkDay)
                                currentScreen = Screen.Home
                            },
                            onDelete = {
                                calendarViewModel.deleteWorkDay(date)
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
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8ECEF)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(Res.drawable.logo_full),
                contentDescription = "TimeCraft",
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(color = Color(0xFF1A3A5A))
        }
    }
}
