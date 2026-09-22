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
import com.emeric.timecraft.model.ClientSchedule
import com.emeric.timecraft.model.DayType
import com.emeric.timecraft.model.Expense
import com.emeric.timecraft.model.WorkDay
import kotlinx.datetime.LocalDate

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
    var selectedType by remember { mutableStateOf(initialWorkDay?.type ?: DayType.WORKED) }
    
    var clientSchedules by remember {
        mutableStateOf(
            if (initialWorkDay != null && initialWorkDay.clientSchedules.isNotEmpty()) {
                initialWorkDay.clientSchedules
            } else if (initialWorkDay != null && initialWorkDay.clients.isNotEmpty()) {
                initialWorkDay.clients.map { ClientSchedule(clientName = it) }
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
                title = { Text("${date.dayOfMonth}/${date.monthNumber}/${date.year}", fontWeight = FontWeight.Bold) },
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

            // Section Clients & Horaires
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Clients & Horaires de travail", fontWeight = FontWeight.Bold)
                            val totalHours = clientSchedules.sumOf { it.calculateHours() }
                            if (totalHours > 0) {
                                val h = totalHours.toInt()
                                val m = ((totalHours - h) * 60).toInt()
                                val formatted = if (m == 0) "${h}h" else "${h}h${m.toString().padStart(2, '0')}"
                                Text("Total journée : $formatted", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1A3A5A), fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            clientSchedules = clientSchedules + ClientSchedule()
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
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                Text("Matin", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A3A5A))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = item.morningStart,
                                        onValueChange = {
                                            clientSchedules = clientSchedules.toMutableList().apply {
                                                set(index, item.copy(morningStart = it))
                                            }
                                        },
                                        label = { Text("Début") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = item.morningEnd,
                                        onValueChange = {
                                            clientSchedules = clientSchedules.toMutableList().apply {
                                                set(index, item.copy(morningEnd = it))
                                            }
                                        },
                                        label = { Text("Fin") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Text("Après-midi", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1A3A5A))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = item.afternoonStart,
                                        onValueChange = {
                                            clientSchedules = clientSchedules.toMutableList().apply {
                                                set(index, item.copy(afternoonStart = it))
                                            }
                                        },
                                        label = { Text("Début") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = item.afternoonEnd,
                                        onValueChange = {
                                            clientSchedules = clientSchedules.toMutableList().apply {
                                                set(index, item.copy(afternoonEnd = it))
                                            }
                                        },
                                        label = { Text("Fin") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
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
                    val validSchedules = clientSchedules.filter { it.clientName.isNotBlank() || it.calculateHours() > 0 }
                    val clientNames = validSchedules.map { it.clientName }.filter { it.isNotBlank() }
                    onSave(
                        WorkDay(
                            userId = "", // Handled by VM
                            date = date,
                            type = selectedType,
                            isWorked = selectedType == DayType.WORKED,
                            clients = clientNames,
                            clientSchedules = validSchedules,
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
