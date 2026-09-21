package com.emeric.timecraft.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emeric.timecraft.getPlatform
import com.emeric.timecraft.viewmodel.CalendarViewModel
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import timecraft.app.generated.resources.Res
import timecraft.app.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    calendarViewModel: CalendarViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToReport: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val platform = getPlatform()
    var currentTab by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8ECEF))
    ) {
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

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when(currentTab) {
                                0 -> "Calendrier"
                                1 -> "Trajets"
                                else -> "Rapport"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { platform.exit() }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Quitter")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            platform.openEmail("3lmunch0@gmail.com", "Support TimeCraft")
                        }) {
                            Icon(Icons.Default.Email, contentDescription = "Support Email")
                        }
                        IconButton(onClick = { onNavigateToSettings() }) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                when (currentTab) {
                    0 -> CalendarScreen(
                        viewModel = calendarViewModel,
                        onDayClick = onDayClick
                    )
                    1 -> MapScreen()
                    2 -> ReportScreen()
                }

                // Floating Bottom Bar
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White.copy(alpha = 0.88f),
                    shadowElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        NavTabItem(0, Icons.Default.CalendarMonth, "Calendrier", currentTab) { currentTab = 0 }
                        NavTabItem(1, Icons.Default.Map, "Trajets", currentTab) { currentTab = 1 }
                        NavTabItem(2, Icons.Default.BarChart, "Rapport", currentTab) { currentTab = 2 }
                    }
                }

                // Kofi Button (Coffee)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 100.dp, start = 20.dp) 
                        .clickable {
                            platform.openUrl("https://ko-fi.com/elmuncho")
                        },
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.7f),
                    shadowElevation = 4.dp
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("☕", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Offrir un café", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavTabItem(index: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, currentTab: Int, onClick: () -> Unit) {
    val isSelected = index == currentTab
    val color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
    
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = color)
        Text(label, fontSize = 9.sp, color = color, maxLines = 1)
    }
}

@Composable
fun ReportScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            Text("Rapports", style = MaterialTheme.typography.titleLarge)
            Text("Statistiques de vos journées", style = MaterialTheme.typography.bodySmall)
        }
    }
}
