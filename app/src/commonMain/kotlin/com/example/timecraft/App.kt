package com.example.timecraft

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timecraft.ui.theme.TimeCraftTheme

import com.example.timecraft.screens.AuthScreen
import com.example.timecraft.network.supabase
import io.github.jan.supabase.gotrue.auth

import com.example.timecraft.screens.CalendarScreen
import com.example.timecraft.viewmodel.CalendarViewModel

import com.example.timecraft.screens.DayDetailScreen
import kotlinx.datetime.LocalDate

enum class Screen {
    Auth, Calendar, DayDetail, Map
}

import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Map

@Composable
fun App() {
    TimeCraftTheme {
        var currentScreen by remember { mutableStateOf(if (supabase.auth.currentSessionOrNull() != null) Screen.Calendar else Screen.Auth) }
        var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
        val calendarViewModel = remember { CalendarViewModel() }

        Scaffold(
            bottomBar = {
                if (currentScreen != Screen.Auth && currentScreen != Screen.DayDetail) {
                    NavigationBar(containerColor = Color.White.copy(alpha = 0.9f)) {
                        NavigationBarItem(
                            selected = currentScreen == Screen.Calendar,
                            onClick = { currentScreen = Screen.Calendar },
                            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            label = { Text("Calendrier") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.Map,
                            onClick = { currentScreen = Screen.Map },
                            icon = { Icon(Icons.Default.Map, contentDescription = null) },
                            label = { Text("Trajets") }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (currentScreen) {
                    Screen.Auth -> AuthScreen(onAuthSuccess = { currentScreen = Screen.Calendar })
                    Screen.Calendar -> CalendarScreen(
                        viewModel = calendarViewModel,
                        onDayClick = { date ->
                            selectedDate = date
                            currentScreen = Screen.DayDetail
                        }
                    )
                    Screen.DayDetail -> selectedDate?.let { date ->
                        DayDetailScreen(
                            date = date,
                            onBack = { currentScreen = Screen.Calendar },
                            onSave = { amount, desc, client ->
                                // TODO: Save to VM/Supabase
                                currentScreen = Screen.Calendar
                            }
                        )
                    }
                    Screen.Map -> MapScreen()
                }
            }
        }
    }
}

@Composable
fun MapScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            Text("Carte des trajets (MapLibre)", style = MaterialTheme.typography.titleLarge)
            Text("Prochainement disponible", style = MaterialTheme.typography.bodySmall)
        }
    }
}
