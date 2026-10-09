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
import com.emeric.timecraft.getPdfExporter
import com.emeric.timecraft.getPlatform
import com.emeric.timecraft.getLocationTracker
import com.emeric.timecraft.getSettingsStorage
import com.emeric.timecraft.model.DayType
import com.emeric.timecraft.utils.DateTimeUtils
import com.emeric.timecraft.viewmodel.CalendarViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import timecraft.app.generated.resources.Res
import timecraft.app.generated.resources.*

import com.emeric.timecraft.utils.*
import kotlin.math.round

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

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
    val strings = getAppStrings(LanguageManager.currentLanguage)
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
                                0 -> strings.calendar
                                1 -> strings.trips
                                else -> strings.report
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { platform.exit() }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = strings.quit)
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
                        NavTabItem(0, Icons.Default.CalendarMonth, strings.calendar, currentTab) { currentTab = 0 }
                        NavTabItem(1, Icons.Default.Map, strings.trips, currentTab) { currentTab = 1 }
                        NavTabItem(2, Icons.Default.BarChart, strings.report, currentTab) { currentTab = 2 }
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
                        Text(strings.buyCoffee, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
    val coroutineScope = rememberCoroutineScope()
    val strings = getAppStrings(LanguageManager.currentLanguage)
    val pdfExporter = remember { getPdfExporter() }
    val locationTracker = remember { getLocationTracker() }
    val trackHistory by locationTracker.trackHistory.collectAsState()

    var reportMode by remember { mutableStateOf(0) } // 0 = Mensuel, 1 = Annuel
    var selectedYear by remember { mutableStateOf(viewModel.currentMonth.year) }

    val currentMonth = viewModel.currentMonth

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
        // Mode Switcher (Mensuel / Annuel)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { reportMode = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (reportMode == 0) Color(0xFF1A3A5A) else Color.White,
                    contentColor = if (reportMode == 0) Color.White else Color(0xFF1A3A5A)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings.monthlyReport, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { reportMode = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (reportMode == 1) Color(0xFF1A3A5A) else Color.White,
                    contentColor = if (reportMode == 1) Color.White else Color(0xFF1A3A5A)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings.yearlyReport, fontWeight = FontWeight.Bold)
            }
        }

        if (reportMode == 0) {
            // RAPPORT MENSUEL
            val settingsStorage = remember { getSettingsStorage() }
            val targetWeeklyHours = remember {
                settingsStorage.getString("weekly_target_hours", "35").toDoubleOrNull() ?: 35.0
            }
            val dailyTargetHours = targetWeeklyHours / 5.0
            val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

            val isCurrentMonth = currentMonth.year == today.year && currentMonth.month == today.month
            val isPastMonth = (currentMonth.year < today.year) || (currentMonth.year == today.year && currentMonth.month < today.month)

            val monthDays = remember(viewModel.workDays, currentMonth) {
                viewModel.workDays.values.filter {
                    it.date.month == currentMonth.month && it.date.year == currentMonth.year
                }
            }

            val monthDates = remember(currentMonth) {
                DateTimeUtils.getDaysInMonth(currentMonth.year, currentMonth.month)
            }
            val daysInMonthCount = monthDates.size
            val startOfMonth = LocalDate(currentMonth.year, currentMonth.month, 1)
            val endOfMonth = LocalDate(currentMonth.year, currentMonth.month, daysInMonthCount)

            val monthGpsPoints = remember(currentMonth, trackHistory) {
                val stored = GpsStorage.getPointsForPeriod(startOfMonth, endOfMonth)
                if (stored.isNotEmpty()) stored else trackHistory
            }

            val totalMonthDistanceKm = remember(monthDates, viewModel.workDays, trackHistory) {
                calculateTotalPeriodDistanceKm(monthDates, viewModel.workDays, trackHistory)
            }

            val workedDaysCount = monthDays.count { it.type == DayType.WORKED }
            val rttDaysCount = monthDays.count { it.type == DayType.RTT }
            val leaveDaysCount = monthDays.count { it.type == DayType.PAID_LEAVE }
            val familyDaysCount = monthDays.count { it.type == DayType.FAMILY_ABSENCE }
            val unpaidDaysCount = monthDays.count { it.type == DayType.UNPAID_LEAVE }

            val totalHoursWorked = monthDays.filter { it.type == DayType.WORKED }.sumOf { it.totalWorkedHours() }
            val totalExpenses = monthDays.flatMap { it.expenses }.sumOf { it.amount }

            val monthDaysMap = remember(monthDays) { monthDays.associateBy { it.date } }

            val allBusinessDaysInMonth = remember(monthDates) {
                monthDates.filter { date ->
                    date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY && !FrenchHolidays.isHoliday(date)
                }
            }

            val elapsedBusinessDays = remember(allBusinessDaysInMonth, today, isCurrentMonth, isPastMonth) {
                if (isPastMonth) {
                    allBusinessDaysInMonth
                } else if (isCurrentMonth) {
                    allBusinessDaysInMonth.filter { it <= today }
                } else {
                    emptyList()
                }
            }
            val elapsedBusinessDaysCount = elapsedBusinessDays.size

            val creditedDaysTypes = setOf(DayType.PAID_LEAVE, DayType.RTT, DayType.FAMILY_ABSENCE)

            var elapsedCreditedDaysCount = 0
            var remainingCreditedDaysCount = 0

            allBusinessDaysInMonth.forEach { date ->
                val dayObj = monthDaysMap[date]
                if (dayObj != null && dayObj.type in creditedDaysTypes) {
                    if (date <= today || isPastMonth) {
                        elapsedCreditedDaysCount++
                    } else {
                        remainingCreditedDaysCount++
                    }
                }
            }

            val totalCreditedDaysCount = elapsedCreditedDaysCount + remainingCreditedDaysCount
            val creditedHoursTotal = totalCreditedDaysCount * dailyTargetHours

            val elapsedCreditedHours = elapsedCreditedDaysCount * dailyTargetHours
            val totalEquivalentHoursElapsed = totalHoursWorked + elapsedCreditedHours

            val weeksInMonth = daysInMonthCount / 7.0
            val elapsedWeeks = if (isPastMonth) {
                weeksInMonth
            } else if (isCurrentMonth) {
                kotlin.math.max(elapsedBusinessDaysCount / 5.0, 0.2)
            } else {
                0.2
            }

            val currentWeeklyAvg = if (isPastMonth || isCurrentMonth) {
                totalEquivalentHoursElapsed / elapsedWeeks
            } else {
                0.0
            }

            val diffVsTarget = currentWeeklyAvg - targetWeeklyHours

            // Projection
            val remainingBusinessDaysCount = allBusinessDaysInMonth.count { it > today }
            val remainingPlannedWorkDays = kotlin.math.max(0, remainingBusinessDaysCount - remainingCreditedDaysCount)
            val projectedRemainingHours = remainingPlannedWorkDays * dailyTargetHours

            val projectedMonthTotalHours = totalHoursWorked + creditedHoursTotal + projectedRemainingHours
            val projectedWeeklyAvg = if (weeksInMonth > 0) projectedMonthTotalHours / weeksInMonth else 0.0

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

            // Weekly Average & Projection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Moyenne Hebdomadaire",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A3A5A)
                            )
                            Text(
                                if (isCurrentMonth) "Rythme actuel ($elapsedBusinessDaysCount j. ouvrés écoulés)" else "Base mensuelle (${(round(weeksInMonth * 10.0) / 10.0)} sem.)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${formatH(currentWeeklyAvg)} / sem",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1A3A5A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = if (diffVsTarget >= -0.1) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                val targetStr = formatH(targetWeeklyHours)
                                val diffTextStr = when {
                                    kotlin.math.abs(diffVsTarget) < 0.1 -> "🎯 Réf. $targetStr atteinte"
                                    diffVsTarget > 0 -> "📈 +${formatH(diffVsTarget)} vs $targetStr"
                                    else -> "📉 -${formatH(-diffVsTarget)} vs $targetStr"
                                }
                                Text(
                                    diffTextStr,
                                    color = if (diffVsTarget >= -0.1) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (isCurrentMonth) {
                        HorizontalDivider(color = Color(0xFFE0E0E0))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Projection fin de mois",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A3A5A)
                                )
                            }
                            Text(
                                "${formatH(projectedMonthTotalHours)} total (~${formatH(projectedWeeklyAvg)}/sem)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                        }
                    }

                    if (totalCreditedDaysCount > 0) {
                        Text(
                            "💡 Inclus $totalCreditedDaysCount j. de congés/RTT (${formatH(creditedHoursTotal)} équivalentes)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            val formattedWeeklyAvg = formatH(currentWeeklyAvg)
            val targetStr = formatH(targetWeeklyHours)

            // Export PDF Button
            Button(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        val metrics = listOf(
                            "Jours travaillés" to "$workedDaysCount jours (${formatH(totalHoursWorked)})",
                            "Moyenne hebdo à ce jour" to "$formattedWeeklyAvg / sem (réf. $targetStr)",
                            "Projection fin de mois" to "${formatH(projectedMonthTotalHours)} total (~${formatH(projectedWeeklyAvg)}/sem)",
                            "RTT pris" to "$rttDaysCount jours",
                            "Congés payés" to "$leaveDaysCount jours",
                            "Evénement familial" to "$familyDaysCount jours",
                            "Congé sans solde" to "$unpaidDaysCount jours",
                            "Distance de déplacement" to "${(round(totalMonthDistanceKm * 10.0) / 10.0)} km",
                            "Total des frais" to "$totalExpenses €"
                        )
                        val clientBreakdown = clientHoursMap.map { (client, hrs) -> client to formatH(hrs) }
                        pdfExporter.exportReportPdf(
                            title = "Rapport Mensuel - ${DateTimeUtils.getMonthName(currentMonth.month)} ${currentMonth.year}",
                            subtitle = "Généré par TimeCraft - Suivi d'activité",
                            metrics = metrics,
                            clientBreakdown = clientBreakdown,
                            trackPoints = monthGpsPoints
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A5A))
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exporter le Rapport Mensuel en PDF", fontWeight = FontWeight.Bold)
            }

            // Days Type Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Synthèse Mensuelle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A3A5A))

                    ReportRow("💼 Jours travaillés", "$workedDaysCount jours (${formatH(totalHoursWorked)})", DayType.WORKED.getColor())
                    ReportRow("⏱️ Moyenne hebdo (réf. 35h)", "$formattedWeeklyAvg / sem", Color(0xFF1976D2))
                    ReportRow("⏱️ RTT pris", "$rttDaysCount jours", DayType.RTT.getColor())
                    ReportRow("🌴 Congés payés", "$leaveDaysCount jours", DayType.PAID_LEAVE.getColor())
                    ReportRow("👨‍👩‍👧 Evénement familial", "$familyDaysCount jours", DayType.FAMILY_ABSENCE.getColor())
                    ReportRow("🚫 Congé sans solde", "$unpaidDaysCount jours", DayType.UNPAID_LEAVE.getColor())

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

                    ReportRow("🚗 Distance de déplacement", "${(round(totalMonthDistanceKm * 10.0) / 10.0)} km", Color(0xFF1976D2))
                    ReportRow("💶 Total des frais engagés", "$totalExpenses €", Color(0xFF388E3C))
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
        } else {
            // RAPPORT ANNUEL
            val startOfYear = LocalDate(selectedYear, 1, 1)
            val endOfYear = LocalDate(selectedYear, 12, 31)

            val yearDays = remember(viewModel.workDays, selectedYear) {
                viewModel.workDays.values.filter { it.date.year == selectedYear }
            }

            val yearDates = remember(selectedYear) {
                Month.entries.flatMap { month ->
                    DateTimeUtils.getDaysInMonth(selectedYear, month)
                }
            }

            val yearGpsPoints = remember(selectedYear, trackHistory) {
                val stored = GpsStorage.getPointsForPeriod(startOfYear, endOfYear)
                if (stored.isNotEmpty()) stored else trackHistory
            }

            val totalYearDistanceKm = remember(yearDates, viewModel.workDays, trackHistory) {
                calculateTotalPeriodDistanceKm(yearDates, viewModel.workDays, trackHistory)
            }

            val totalWorkedDaysYear = yearDays.count { it.type == DayType.WORKED }
            val totalHoursYear = yearDays.filter { it.type == DayType.WORKED }.sumOf { it.totalWorkedHours() }
            val totalRttYear = yearDays.count { it.type == DayType.RTT }
            val totalLeaveYear = yearDays.count { it.type == DayType.PAID_LEAVE }
            val totalExpensesYear = yearDays.flatMap { it.expenses }.sumOf { it.amount }

            val yearlyWeeklyAvg = totalHoursYear / 52.0

            // Year Selector Card
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
                    IconButton(onClick = { selectedYear -= 1 }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Année précédente")
                    }
                    Text(
                        "Année $selectedYear",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A3A5A)
                    )
                    IconButton(onClick = { selectedYear += 1 }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Année suivante")
                    }
                }
            }

            // Export PDF Button Annuel
            Button(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        val metrics = listOf(
                            "Total Jours travaillés ($selectedYear)" to "$totalWorkedDaysYear jours (${formatH(totalHoursYear)})",
                            "Moyenne hebdo sur l'année" to "${formatH(yearlyWeeklyAvg)} / sem (réf. 35h)",
                            "Total RTT pris" to "$totalRttYear jours",
                            "Total Congés payés" to "$totalLeaveYear jours",
                            "Distance GPS / Déplacement" to "${(round(totalYearDistanceKm * 10.0) / 10.0)} km",
                            "Total des frais" to "$totalExpensesYear €"
                        )
                        pdfExporter.exportReportPdf(
                            title = "Rapport Annuel - Année $selectedYear",
                            subtitle = "Généré par TimeCraft - Bilan annuel",
                            metrics = metrics,
                            clientBreakdown = emptyList(),
                            trackPoints = yearGpsPoints
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A5A))
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exporter le Bilan Annuel en PDF", fontWeight = FontWeight.Bold)
            }

            // Yearly Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Bilan Annuel $selectedYear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A3A5A))

                    ReportRow("💼 Jours travaillés", "$totalWorkedDaysYear jours (${formatH(totalHoursYear)})", DayType.WORKED.getColor())
                    ReportRow("⏱️ Moyenne hebdo sur l'année", "${formatH(yearlyWeeklyAvg)} / sem", Color(0xFF1976D2))
                    ReportRow("⏱️ RTT pris", "$totalRttYear jours", DayType.RTT.getColor())
                    ReportRow("🌴 Congés payés", "$totalLeaveYear jours", DayType.PAID_LEAVE.getColor())

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))

                    ReportRow("🚗 Distance de déplacement", "${(round(totalYearDistanceKm * 10.0) / 10.0)} km", Color(0xFF1976D2))
                    ReportRow("💶 Total des frais de l'année", "$totalExpensesYear €", Color(0xFF388E3C))
                }
            }

            // Month-by-month tiles (12 Month Cards)
            Text("Détail Mois par Mois ($selectedYear)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A3A5A))

            (1..12).forEach { monthIdx ->
                val monthEnum = kotlinx.datetime.Month.entries[monthIdx - 1]
                val mDaysInMonth = DateTimeUtils.getDaysInMonth(selectedYear, monthEnum).size
                val mWeeks = mDaysInMonth / 7.0
                val mDays = yearDays.filter { it.date.monthNumber == monthIdx }
                val mWorked = mDays.count { it.type == DayType.WORKED }
                val mHours = mDays.filter { it.type == DayType.WORKED }.sumOf { it.totalWorkedHours() }
                val mWeeklyAvg = if (mWeeks > 0) mHours / mWeeks else 0.0
                val mExpenses = mDays.flatMap { it.expenses }.sumOf { it.amount }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(DateTimeUtils.getMonthName(monthEnum), fontWeight = FontWeight.Bold, color = Color(0xFF1A3A5A))
                            Text("$mWorked jours travaillés (${formatH(mHours)}) • ${formatH(mWeeklyAvg)}/sem", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        if (mExpenses > 0) {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("$mExpenses €", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
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

private fun calculateTotalPeriodDistanceKm(
    dates: List<LocalDate>,
    workDaysMap: Map<LocalDate, com.emeric.timecraft.model.WorkDay>,
    trackHistory: List<com.emeric.timecraft.LocationPoint>
): Double {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val trackedDatesSet = GpsStorage.getAllTrackedDates().toSet()
    var totalKm = 0.0

    for (date in dates) {
        val workDay = workDaysMap[date]
        val manualKm = workDay?.distanceKm ?: 0.0
        if (manualKm > 0.0) {
            totalKm += manualKm
        } else {
            val dateStr = date.toString()
            val points = if (trackedDatesSet.contains(dateStr)) {
                GpsStorage.getPointsForDate(dateStr)
            } else if (date == today) {
                trackHistory
            } else {
                emptyList()
            }
            if (points.isNotEmpty()) {
                totalKm += GpsStorage.calculateDistanceKm(points)
            }
        }
    }
    return totalKm
}
