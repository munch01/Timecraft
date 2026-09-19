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

@Composable
fun App() {
    TimeCraftTheme {
        var isAuthenticated by remember { mutableStateOf(supabase.auth.currentSessionOrNull() != null) }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (!isAuthenticated) {
                AuthScreen(onAuthSuccess = { isAuthenticated = true })
            } else {
                MainContent(onLogout = { isAuthenticated = false })
            }
        }
    }
}

@Composable
fun MainContent(onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Calendrier TimeCraft", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onLogout) {
            Text("Se déconnecter")
        }
    }
}
