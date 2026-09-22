package com.emeric.timecraft.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emeric.timecraft.getSettingsStorage
import com.emeric.timecraft.model.ClientSchedule
import com.emeric.timecraft.model.DayType
import com.emeric.timecraft.model.DefaultSchedules
import com.emeric.timecraft.model.Expense
import com.emeric.timecraft.model.WorkDay
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val timeOptions = remember {
        val list = mutableListOf<String>()
        for (h in 6..22) {
            for (m in listOf(0, 15, 30, 45)) {
                val hStr = h.toString().padStart(2, '0')
                val mStr = m.toString().padStart(2, '0')
                list.add("$hStr:$mStr")
            }
        }
        list
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.AccessTime, contentDescription = "Tranches de 15 min")
                }
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 220.dp)
        ) {
            timeOptions.forEach { timeOption ->
                DropdownMenuItem(
                    text = { Text(timeOption, fontWeight = FontWeight.Bold, color = Color(0xFF1A3A5A)) },
                    onClick = {
                        onValueChange(timeOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: LocalDate,
    initialWorkDay: WorkDay?,
    onBack: () -> Unit,
    onSave: (WorkDay) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settingsStorage = remember { getSettingsStorage() }
    val dayOfWeekName = date.dayOfWeek.name
    val defaultConfig = remember(date) { DefaultSchedules.getScheduleForDay(dayOfWeekName, settingsStorage) }

    var selectedType by remember { mutableStateOf(initialWorkDay?.type ?: DayType.WORKED) }
    
    var clientSchedules by remember {
        mutableStateOf(
            if (initialWorkDay != null && initialWorkDay.clients.isNotEmpty()) {
                initialWorkDay.getEffectiveSchedules()
            } else if (defaultConfig.isWorkDay) {
                listOf(
                    ClientSchedule(
                        clientName = "",
                        morningStart = defaultConfig.morningStart,
                        morningEnd = defaultConfig.morningEnd,
                        afternoonStart = defaultConfig.afternoonStart,
                        afternoonEnd = defaultConfig.afternoonEnd
                    )
                )
            } else {
                emptyList()
            }
        )
    }

    var expenses by remember { mutableStateOf(initialWorkDay?.expenses ?: emptyList<Expense>()) }
    
    var showTypeMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFFE8ECEF),
        topBar = {
            TopAppBar(
                title = { Text("${date.dayOfMonth}/${date.monthNumber}/${date.year} (${defaultConfig.label})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (initialWorkDay != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Type de journée", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A3A5A))
            
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showTypeMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = selectedType.getColor())
                ) {
                    Text(selectedType.label, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                
                DropdownMenu(
                    expanded = showTypeMenu,
                    onDismissRequest = { showTypeMenu = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    DayType.entries.filter { it != DayType.NONE }.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label, color = type.getColor(), fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedType = type
                                showTypeMenu = false
                            }
                        )
                    }
                }
            }

            if (selectedType != DayType.WORKED) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = selectedType.getColor().copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = selectedType.getColor())
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Journée enregistrée comme : ${selectedType.label}",
                            fontWeight = FontWeight.Bold,
                            color = selectedType.getColor()
                        )
                    }
                }
            } else {
                // Section Clients & Horaires (for WORKED days)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Clients & Horaires de travail", fontWeight = FontWeight.Bold)
                                
                                val actualHours = clientSchedules.sumOf { it.calculateHours() }
                                val targetHours = defaultConfig.calculateTargetHours()
                                val diffHours = actualHours - targetHours

                                val formatH = { hrs: Double ->
                                    val absHrs = kotlin.math.abs(hrs)
                                    val h = absHrs.toInt()
                                    val m = ((absHrs - h) * 60).toInt()
                                    if (m == 0) "${h}h" else "${h}h${m.toString().padStart(2, '0')}"
                                }

                                if (actualHours > 0 || targetHours > 0) {
                                    Text(
                                        "Total : ${formatH(actualHours)} (Prévu ${defaultConfig.label} : ${formatH(targetHours)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF1A3A5A),
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (defaultConfig.isWorkDay && targetHours > 0) {
                                        val overText = when {
                                            diffHours > 0.01 -> "+${formatH(diffHours)} (Heures supplémentaires)"
                                            diffHours < -0.01 -> "-${formatH(diffHours)} (Heures manquantes)"
                                            else -> "Conforme aux horaires prévus"
                                        }
                                        val overColor = when {
                                            diffHours > 0.01 -> Color(0xFF2E7D32)
                                            diffHours < -0.01 -> Color(0xFFE65100)
                                            else -> Color.Gray
                                        }
                                        Text(overText, style = MaterialTheme.typography.labelSmall, color = overColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            IconButton(onClick = {
                                clientSchedules = clientSchedules + ClientSchedule(
                                    clientName = "",
                                    morningStart = defaultConfig.morningStart,
                                    morningEnd = defaultConfig.morningEnd,
                                    afternoonStart = defaultConfig.afternoonStart,
                                    afternoonEnd = defaultConfig.afternoonEnd
                                )
                            }) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Ajouter client", tint = Color(0xFF1A3A5A))
                            }
                        }

                        if (clientSchedules.isEmpty()) {
                            Text("Aucun client saisi. Cliquez sur + pour ajouter un client et ses horaires.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        clientSchedules.forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F6F8)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = item.clientName,
                                            onValueChange = { newName ->
                                                clientSchedules = clientSchedules.toMutableList().apply {
                                                    set(index, item.copy(clientName = newName))
                                                }
                                            },
                                            label = { Text("Nom du client ${index + 1}") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        IconButton(onClick = {
                                            clientSchedules = clientSchedules.filterIndexed { i, _ -> i != index }
                                        }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer le client", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    val hasMorning = item.morningStart.isNotBlank() || item.morningEnd.isNotBlank()
                                    val hasAfternoon = item.afternoonStart.isNotBlank() || item.afternoonEnd.isNotBlank()

                                    // Section Matin
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Matin", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A3A5A))
                                        if (hasMorning) {
                                            TextButton(
                                                onClick = {
                                                    clientSchedules = clientSchedules.toMutableList().apply {
                                                        set(index, item.copy(morningStart = "", morningEnd = ""))
                                                    }
                                                },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Supprimer le matin", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }

                                    if (hasMorning) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TimeInputField(
                                                value = item.morningStart,
                                                onValueChange = { newStart ->
                                                    clientSchedules = clientSchedules.toMutableList().apply {
                                                        set(index, item.copy(morningStart = newStart))
                                                    }
                                                },
                                                label = "Début",
                                                modifier = Modifier.weight(1f)
                                            )
                                            TimeInputField(
                                                value = item.morningEnd,
                                                onValueChange = { newEnd ->
                                                    clientSchedules = clientSchedules.toMutableList().apply {
                                                        set(index, item.copy(morningEnd = newEnd))
                                                    }
                                                },
                                                label = "Fin",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                clientSchedules = clientSchedules.toMutableList().apply {
                                                    set(index, item.copy(morningStart = defaultConfig.morningStart, morningEnd = defaultConfig.morningEnd))
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ajouter les horaires du matin", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Section Après-midi
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Après-midi", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A3A5A))
                                        if (hasAfternoon) {
                                            TextButton(
                                                onClick = {
                                                    clientSchedules = clientSchedules.toMutableList().apply {
                                                        set(index, item.copy(afternoonStart = "", afternoonEnd = ""))
                                                    }
                                                },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Supprimer l'après-midi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }

                                    if (hasAfternoon) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TimeInputField(
                                                value = item.afternoonStart,
                                                onValueChange = { newStart ->
                                                    clientSchedules = clientSchedules.toMutableList().apply {
                                                        set(index, item.copy(afternoonStart = newStart))
                                                    }
                                                },
                                                label = "Début",
                                                modifier = Modifier.weight(1f)
                                            )
                                            TimeInputField(
                                                value = item.afternoonEnd,
                                                onValueChange = { newEnd ->
                                                    clientSchedules = clientSchedules.toMutableList().apply {
                                                        set(index, item.copy(afternoonEnd = newEnd))
                                                    }
                                                },
                                                label = "Fin",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                clientSchedules = clientSchedules.toMutableList().apply {
                                                    set(index, item.copy(afternoonStart = defaultConfig.afternoonStart, afternoonEnd = defaultConfig.afternoonEnd))
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ajouter les horaires de l'après-midi", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    val itemHours = item.formattedHours()
                                    if (item.calculateHours() > 0) {
                                        Text(
                                            "Durée travaillée pour ce client : $itemHours",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1A3A5A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section Frais
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Frais engagés", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { expenses = expenses + Expense(amount = 0.0, description = "") }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Ajouter", tint = Color(0xFF1A3A5A))
                        }
                    }
                    
                    expenses.forEachIndexed { index, expense ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = if (expense.amount == 0.0) "" else expense.amount.toString(),
                                    onValueChange = { newValue ->
                                        val amt = newValue.toDoubleOrNull() ?: 0.0
                                        expenses = expenses.toMutableList().apply { set(index, expense.copy(amount = amt)) }
                                    },
                                    label = { Text("Montant (€)") },
                                    modifier = Modifier.weight(1f),
                                    leadingIcon = { Icon(Icons.Default.Euro, contentDescription = null) }
                                )
                                IconButton(onClick = { expenses = expenses.filterIndexed { i, _ -> i != index } }) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Supprimer")
                                }
                            }
                            OutlinedTextField(
                                value = expense.description,
                                onValueChange = { newValue ->
                                    expenses = expenses.toMutableList().apply { set(index, expense.copy(description = newValue)) }
                                },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (index < expenses.size - 1) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val validSchedules = if (selectedType == DayType.WORKED) {
                        clientSchedules.filter { it.clientName.isNotBlank() || it.calculateHours() > 0 }
                    } else emptyList()
                    val encodedClients = validSchedules.map { it.toSerializedString() }
                    onSave(
                        WorkDay(
                            userId = "", // Handled by VM
                            date = date,
                            type = selectedType,
                            isWorked = selectedType == DayType.WORKED,
                            clients = encodedClients,
                            expenses = expenses.filter { it.description.isNotBlank() || it.amount > 0 }
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A3A5A))
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enregistrer")
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Supprimer cette journée ?") },
            text = { Text("Toutes les données saisies (frais, clients) seront effacées.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Annuler") }
            }
        )
    }
}
