package com.example.timecraft.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timecraft.utils.DateTimeUtils
import com.example.timecraft.viewmodel.CalendarViewModel
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonth = viewModel.currentMonth
    val days = DateTimeUtils.getDaysInMonth(currentMonth.year, currentMonth.month)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "${DateTimeUtils.getMonthName(currentMonth.month)} ${currentMonth.year}",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onPreviousMonth() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Précédent")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onNextMonth() }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Suivant")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Jours de la semaine
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim").forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.padding(8.dp).height(350.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    // Empty spaces for the first day offset
                    val firstDayOfWeek = days.first().dayOfWeek.ordinal // 0 = Monday
                    items(firstDayOfWeek) {
                        Box(modifier = Modifier.size(40.dp))
                    }

                    items(days) { date ->
                        val workDay = viewModel.workDays[date]
                        DayItem(
                            date = date,
                            workDay = workDay,
                            onClick = { 
                                onDayClick(date)
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Statistiques du mois
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Jours travaillés", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${viewModel.workDays.values.count { it.date.month == currentMonth.month && it.date.year == currentMonth.year && it.type == com.example.timecraft.model.DayType.WORKED }} jours ce mois-ci", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color(0xFFE8ECEF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = viewModel.workDays.values.count { it.date.month == currentMonth.month && it.date.year == currentMonth.year && it.type == com.example.timecraft.model.DayType.WORKED }.toString(),
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1A3A5A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayItem(
    date: LocalDate,
    workDay: com.example.timecraft.model.WorkDay?,
    onClick: () -> Unit
) {
    val isMarked = workDay != null
    val bgColor = workDay?.type?.getColor() ?: Color.Transparent
    val textColor = if (isMarked) Color.White else Color.Black

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = textColor,
            fontWeight = if (isMarked) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}
