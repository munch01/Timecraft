package com.emeric.timecraft.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emeric.timecraft.getBiometryManager
import com.emeric.timecraft.getPlatform
import com.emeric.timecraft.getSettingsStorage
import com.emeric.timecraft.network.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserUpdateBuilder
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import timecraft.app.generated.resources.Res
import timecraft.app.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit
) {
    val platform = getPlatform()
    val settingsStorage = remember { getSettingsStorage() }
    val biometryManager = remember { getBiometryManager() }
    val coroutineScope = rememberCoroutineScope()
    val currentUser = supabase.auth.currentSessionOrNull()?.user
    val appVersion = "1.0.2"

    var isBiometricEnabled by remember { mutableStateOf(settingsStorage.getBoolean("biometric_enabled", false)) }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    
    var newEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    val availableLanguages = listOf("Français", "English", "Español")
    var selectedLanguage by remember { mutableStateOf("Français") }

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

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Paramètres", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Profil & Compte",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f))
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Column {
                                Text(text = currentUser?.email ?: "Utilisateur", fontWeight = FontWeight.Bold)
                                Text(text = "Connecté via Supabase", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsClickableItem(
                            icon = Icons.Default.Email,
                            title = "Adresse e-mail",
                            subtitle = "Modifier votre adresse e-mail",
                            onClick = { showEmailDialog = true }
                        )
                    }
                }

                Text(
                    text = "Sécurité",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f))
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Verrouillage biométrique", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (isBiometricEnabled) "Activé au démarrage" else "Désactivé",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        if (biometryManager != null && biometryManager.canAuthenticate()) {
                                            biometryManager.authenticate(
                                                title = "Confirmation biométrique",
                                                subtitle = "Validez pour activer le verrouillage biométrique",
                                                negativeButtonText = "Annuler",
                                                onSuccess = {
                                                    settingsStorage.setBoolean("biometric_enabled", true)
                                                    isBiometricEnabled = true
                                                    platform.showToast("Verrouillage biométrique activé")
                                                },
                                                onError = { err ->
                                                    platform.showToast("Erreur : $err")
                                                }
                                            )
                                        } else {
                                            platform.showToast("La biométrie n'est pas disponible ou configurée sur cet appareil")
                                        }
                                    } else {
                                        settingsStorage.setBoolean("biometric_enabled", false)
                                        isBiometricEnabled = false
                                        platform.showToast("Verrouillage biométrique désactivé")
                                    }
                                }
                            ) 
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsClickableItem(
                            icon = Icons.Default.Lock,
                            title = "Mot de passe",
                            subtitle = "Changer votre mot de passe",
                            onClick = { showPasswordDialog = true }
                        )
                    }
                }

                Text(
                    text = "Horaires par défaut (par jour)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Configurez vos horaires habituels pour chaque jour de la semaine (utilisés pour pré-remplir la journée et calculer les heures supplémentaires).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        var daySchedules by remember {
                            mutableStateOf(
                                com.emeric.timecraft.model.DefaultSchedules.defaultDays.map { defaultItem ->
                                    com.emeric.timecraft.model.DefaultSchedules.getScheduleForDay(defaultItem.dayOfWeekName, settingsStorage)
                                }
                            )
                        }

                        daySchedules.forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F6F8)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF1A3A5A))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (item.isWorkDay) "Travaillé" else "Repos", style = MaterialTheme.typography.bodySmall, color = if (item.isWorkDay) Color(0xFF1A3A5A) else Color.Gray)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Switch(
                                                checked = item.isWorkDay,
                                                onCheckedChange = { isChecked ->
                                                    val updated = item.copy(isWorkDay = isChecked)
                                                    daySchedules = daySchedules.toMutableList().apply { set(index, updated) }
                                                    com.emeric.timecraft.model.DefaultSchedules.saveScheduleForDay(updated, settingsStorage)
                                                }
                                            )
                                        }
                                    }

                                    if (item.isWorkDay) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = item.morningStart,
                                                onValueChange = { newVal ->
                                                    val updated = item.copy(morningStart = newVal)
                                                    daySchedules = daySchedules.toMutableList().apply { set(index, updated) }
                                                    com.emeric.timecraft.model.DefaultSchedules.saveScheduleForDay(updated, settingsStorage)
                                                },
                                                label = { Text("Matin début") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = item.morningEnd,
                                                onValueChange = { newVal ->
                                                    val updated = item.copy(morningEnd = newVal)
                                                    daySchedules = daySchedules.toMutableList().apply { set(index, updated) }
                                                    com.emeric.timecraft.model.DefaultSchedules.saveScheduleForDay(updated, settingsStorage)
                                                },
                                                label = { Text("Matin fin") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = item.afternoonStart,
                                                onValueChange = { newVal ->
                                                    val updated = item.copy(afternoonStart = newVal)
                                                    daySchedules = daySchedules.toMutableList().apply { set(index, updated) }
                                                    com.emeric.timecraft.model.DefaultSchedules.saveScheduleForDay(updated, settingsStorage)
                                                },
                                                label = { Text("A.M. début") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = item.afternoonEnd,
                                                onValueChange = { newVal ->
                                                    val updated = item.copy(afternoonEnd = newVal)
                                                    daySchedules = daySchedules.toMutableList().apply { set(index, updated) }
                                                    com.emeric.timecraft.model.DefaultSchedules.saveScheduleForDay(updated, settingsStorage)
                                                },
                                                label = { Text("A.M. fin") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                        }

                                        val targetHrs = item.calculateTargetHours()
                                        if (targetHrs > 0) {
                                            val h = targetHrs.toInt()
                                            val m = ((targetHrs - h) * 60).toInt()
                                            val formatted = if (m == 0) "${h}h" else "${h}h${m.toString().padStart(2, '0')}"
                                            Text("Durée prévue : $formatted", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A3A5A))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Préférences",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f))
                ) {
                    SettingsClickableItem(
                        icon = Icons.Default.Language,
                        title = "Langue de l'application",
                        subtitle = selectedLanguage,
                        onClick = { showLanguageDialog = true }
                    )
                }

                Text(
                    text = "À propos & Légal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f))
                ) {
                    Column {
                        SettingsClickableItem(
                            icon = Icons.Default.Code,
                            title = "Projet GitHub",
                            subtitle = "Consulter le code source",
                            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                            onClick = { platform.openUrl("https://github.com/munch01/Timecraft") }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsClickableItem(
                            icon = Icons.Default.PrivacyTip,
                            title = "Politique de confidentialité (RGPD)",
                            subtitle = "Gestion et protection de vos données",
                            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                            onClick = { platform.openUrl("https://github.com/munch01/Timecraft/blob/master/PRIVACY_POLICY.md") }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsClickableItem(
                            icon = Icons.Default.Info,
                            title = "À propos de TimeCraft",
                            subtitle = "Version $appVersion",
                            onClick = { showAboutDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            supabase.auth.signOut()
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Se déconnecter")
                }

                Button(
                    onClick = { showDeleteAccountDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Supprimer mon compte")
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Modifier l'email") },
            text = {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("Nouvel email") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        try {
                            supabase.auth.updateUser {
                                email = newEmail
                            }
                            platform.showToast("Lien de confirmation envoyé au nouvel email")
                            showEmailDialog = false
                        } catch (e: Exception) {
                            platform.showToast("Erreur : ${e.message}")
                        }
                    }
                }) { Text("Modifier") }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Modifier le mot de passe") },
            text = {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nouveau mot de passe") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        try {
                            supabase.auth.updateUser {
                                password = newPassword
                            }
                            platform.showToast("Mot de passe mis à jour")
                            showPasswordDialog = false
                        } catch (e: Exception) {
                            platform.showToast("Erreur : ${e.message}")
                        }
                    }
                }) { Text("Modifier") }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Choisir la langue") },
            text = {
                Column {
                    availableLanguages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLanguage = lang
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (lang == selectedLanguage), onClick = {
                                selectedLanguage = lang
                                showLanguageDialog = false
                            })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = lang, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text("À propos de TimeCraft") },
            text = {
                Column {
                    Text("Application de gestion de temps de travail.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Version $appVersion", fontWeight = FontWeight.Bold)
                    Text("Architecture MVVM avec Jetpack Compose & Supabase.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Fermer") }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Supprimer définitivement le compte ?") },
            text = { Text("Cette action est irréversible. Toutes vos données seront effacées.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                // Supabase doesn't have a direct "delete current user" in client SDK easily for security
                                // Often handled via an Edge Function or Admin API.
                                // For now, we logout or show a warning.
                                platform.showToast("Action nécessitant une validation admin")
                                showDeleteAccountDialog = false
                            } catch (e: Exception) {
                                platform.showToast("Erreur : ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirmer la suppression")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailingIcon: ImageVector = Icons.Default.ChevronRight,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        Icon(trailingIcon, contentDescription = null, tint = Color.Gray)
    }
}
