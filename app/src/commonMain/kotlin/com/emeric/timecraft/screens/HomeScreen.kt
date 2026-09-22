package com.emeric.timecraft.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.emeric.timecraft.model.DayType
import com.emeric.timecraft.utils.DateTimeUtils
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
                    2 -> ReportScreen(viewModel = calendarViewModel)
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
fun ReportScreen(viewModel: CalendarViewModel) {
    val currentMonth = viewModel.currentMonth
    val monthDays = remember(viewModel.workDays, currentMonth) {
        viewModel.workDays.values.filter {
            it.date.month == currentMonth.month && it.date.year == currentMonth.year
        }
    }

    val workedDaysCount = monthDays.count { it.type == DayType.WORKED }
    val rttDaysCount = monthDays.count { it.type == DayType.RTT }
    val leaveDaysCount = monthDays.count { it.type == DayType.PAID_LEAVE }
    val familyDaysCount = monthDays.count { it.type == DayType.FAMILY_ABSENCE }
    val unpaidDaysCount = monthDays.count { it.type == DayType.UNPAID_LEAVE }

    val totalHoursWorked = monthDays.filter { it.type == DayType.WORKED }.sumOf { it.totalWorkedHours() }
    val totalExpenses = monthDays.flatMap { it.expenses }.sumOf { it.amount }

    // Group hours by client
    val clientHoursMap = remember(monthDays) {
        val map = mutableMapOf<String, Double>()
        monthDays.filter { it.type == DayType.WORKED }.forEach { day ->
            day.getEffectiveSchedules().forEach { schedule ->
                if (schedule.clientName.isNotBlank()) {
                    val hrs = schedule.calculateHours()
                    map[schedule.clientName] = (map[schedule.clientName] ?: 0.0) + hrs
                }
            }
        }
        map
    }

    val formatH = { hrs: Double ->
        val h = hrs.toInt()
        val m = ((hrs - h) * 60).toInt()
        if (m == 0) "${h}h" else "${h}h${m.toString().padStart(2, '0')}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 85.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Month Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.onPreviousMonth() }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Précédent")
                }
                Text(
                    "${DateTimeUtils.getMonthName(currentMonth.month)} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A3A5A)
                )
                IconButton(onClick = { viewModel.onNextMonth() }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Suivant")
                }
            }
        }

        // Days Type Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bilan du mois", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A3A5A))

                ReportRow("💼 Jours travaillés", "$workedDaysCount jours (${formatH(totalHoursWorked)})", DayType.WORKED.getColor())
                ReportRow("⏱️ RTT pris", "$rttDaysCount jours", DayType.RTT.getColor())
                ReportRow("🌴 Congés payés", "$leaveDaysCount jours", DayType.PAID_LEAVE.getColor())
                ReportRow("👨‍👩‍👧 Evénement familial", "$familyDaysCount jours", DayType.FAMILY_ABSENCE.getColor())
                ReportRow("🚫 Congé sans solde", "$unpaidDaysCount jours", DayType.UNPAID_LEAVE.getColor())

                if (totalExpenses > 0) {
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                    ReportRow("💶 Total des frais engagés", "$totalExpenses €", Color(0xFF388E3C))
                }
            }
        }

        // Clients Breakdown Card
        if (clientHoursMap.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Répartition par Client", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A3A5A))

                    clientHoursMap.forEach { (clientName, hrs) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(clientName, fontWeight = FontWeight.SemiBold)
                            Surface(
                                color = Color(0xFF1A3A5A),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    formatH(hrs),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(10.dp)) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1A3A5A))
    }
}
