package com.emeric.timecraft.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emeric.timecraft.utils.DateTimeUtils
import com.emeric.timecraft.viewmodel.CalendarViewModel
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    // Automatically refresh data when calendar screen appears
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val currentMonth = viewModel.currentMonth
    val days = DateTimeUtils.getDaysInMonth(currentMonth.year, currentMonth.month)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "${DateTimeUtils.getMonthName(currentMonth.month)} ${currentMonth.year}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Jours de la semaine
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim").forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A3A5A))
                }
            }

            // Enlarged Calendar Card taking full available height
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 80.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val firstDayOfWeek = days.first().dayOfWeek.ordinal // 0 = Monday
                    items(firstDayOfWeek) {
                        Box(modifier = Modifier.aspectRatio(1f))
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
        }
    }
}

@Composable
fun DayItem(
    date: LocalDate,
    workDay: com.emeric.timecraft.model.WorkDay?,
    onClick: () -> Unit
) {
    val isMarked = workDay != null
    val bgColor = workDay?.type?.getColor() ?: Color(0xFFF4F6F8)
    val textColor = if (isMarked) Color.White else Color.Black

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                text = date.dayOfMonth.toString(),
                color = textColor,
                fontWeight = if (isMarked) FontWeight.Bold else FontWeight.Medium,
                fontSize = 16.sp
            )
            if (isMarked && workDay.totalWorkedHours() > 0) {
                val totalH = workDay.formattedTotalHours()
                Text(
                    text = totalH,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
