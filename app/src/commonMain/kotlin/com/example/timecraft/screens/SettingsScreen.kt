package com.example.timecraft.screens

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
import com.example.timecraft.getPlatform
import com.example.timecraft.network.supabase
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
    val coroutineScope = rememberCoroutineScope()
    val currentUser = supabase.auth.currentSessionOrNull()?.user
    val appVersion = "1.0.1"

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
                                Text("Verrouillage biométrique", fontWeight = FontWeight.SemiBold)
                            }
                            Switch(checked = true, onCheckedChange = {}) 
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
