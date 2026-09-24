package com.emeric.timecraft

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emeric.timecraft.network.supabase
import com.emeric.timecraft.screens.*
import com.emeric.timecraft.ui.theme.TimeCraftTheme
import com.emeric.timecraft.viewmodel.CalendarViewModel
import io.github.jan.supabase.auth.auth
import com.emeric.timecraft.utils.*
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import timecraft.app.generated.resources.Res
import timecraft.app.generated.resources.*

enum class Screen {
    Splash, Auth, BiometricUnlock, Home, DayDetail, Settings
}

@Composable
fun App() {
    val currentStrings = getAppStrings(LanguageManager.currentLanguage)
    CompositionLocalProvider(LocalAppStrings provides currentStrings) {
        TimeCraftTheme {
        var currentScreen by remember { mutableStateOf(Screen.Splash) }
        var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
        val calendarViewModel = remember { CalendarViewModel() }

        // Splash screen logic
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000) // 2 seconds splash
            val isLoggedIn = try {
                supabase.auth.currentSessionOrNull() != null
            } catch (e: Exception) {
                false
            }

            if (isLoggedIn) {
                val isBioEnabled = getSettingsStorage().getBoolean("biometric_enabled", false)
                val bioManager = getBiometryManager()
                if (isBioEnabled && bioManager != null && bioManager.canAuthenticate()) {
                    currentScreen = Screen.BiometricUnlock
                } else {
                    currentScreen = Screen.Home
                }
            } else {
                currentScreen = Screen.Auth
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFE8ECEF)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Persistent background for all screens (except splash & biometric unlock)
                if (currentScreen != Screen.Splash && currentScreen != Screen.BiometricUnlock) {
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
                    Screen.BiometricUnlock -> BiometricUnlockScreen(onUnlockSuccess = { currentScreen = Screen.Home })
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
}

@Composable
fun BiometricUnlockScreen(
    onUnlockSuccess: () -> Unit
) {
    val biometryManager = remember { getBiometryManager() }
    val platform = getPlatform()

    fun triggerAuth() {
        biometryManager?.authenticate(
            title = "Déverrouillage TimeCraft",
            subtitle = "Utilisez votre biométrie pour accéder à l'application",
            negativeButtonText = "Annuler",
            onSuccess = { onUnlockSuccess() },
            onError = { platform.showToast(it) }
        )
    }

    LaunchedEffect(Unit) {
        triggerAuth()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8ECEF)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(Res.drawable.logo_full),
                contentDescription = "Logo",
                modifier = Modifier.size(140.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "TimeCraft est verrouillé",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A3A5A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Authentifiez-vous pour continuer",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { triggerAuth() },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A5A))
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Déverrouiller")
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
