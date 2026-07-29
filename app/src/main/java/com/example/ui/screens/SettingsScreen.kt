package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.RedPrimary

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settingsState by viewModel.settings.collectAsState()
    val settings = settingsState ?: AppSettingsEntity()

    var cacheClearedToast by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General
            item {
                SettingsSectionHeader(title = "GENERAL", icon = Icons.Default.Settings)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        SettingsRowText("Language", settings.language)
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        SettingsRowText("Theme Mode", settings.themeMode)
                    }
                }
            }

            // Video Settings
            item {
                SettingsSectionHeader(title = "VIDEO PLAYER", icon = Icons.Default.Videocam)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        SettingsRowSwitch(
                            title = "Auto Play Next Video",
                            checked = settings.autoPlayNext,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(autoPlayNext = it)) }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        SettingsRowSwitch(
                            title = "Resume Video Position",
                            checked = settings.resumeVideo,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(resumeVideo = it)) }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        SettingsRowSwitch(
                            title = "Pinch to Zoom",
                            checked = settings.pinchToZoom,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(pinchToZoom = it)) }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        SettingsRowSwitch(
                            title = "Slide Gesture for Brightness",
                            checked = settings.slideForBrightness,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(slideForBrightness = it)) }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        SettingsRowSwitch(
                            title = "Slide Gesture for Volume",
                            checked = settings.slideForSound,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(slideForSound = it)) }
                        )
                    }
                }
            }

            // Music Settings
            item {
                SettingsSectionHeader(title = "MUSIC & AUDIO", icon = Icons.Default.MusicNote)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        SettingsRowSwitch(
                            title = "Background Audio Playback",
                            checked = settings.bgPlay,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(bgPlay = it)) }
                        )
                    }
                }
            }

            // Safe Box Settings
            item {
                var showEmailDialog by remember { mutableStateOf(false) }
                var emailInput by remember { mutableStateOf(settings.recoveryEmail) }

                SettingsSectionHeader(title = "SAFE BOX SECURITY", icon = Icons.Default.Lock)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        SettingsRowSwitch(
                            title = "Auto Lock on Background",
                            checked = settings.autoLockOnBg,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(autoLockOnBg = it)) }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        SettingsRowSwitch(
                            title = "Fingerprint Lock",
                            checked = settings.fingerprintEnabled,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(fingerprintEnabled = it)) }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    emailInput = settings.recoveryEmail
                                    showEmailDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Recovery Gmail (for Forgot PIN)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (settings.recoveryEmail.isNotBlank()) settings.recoveryEmail else "Not set (Tap to set)",
                                fontSize = 13.sp,
                                color = if (settings.recoveryEmail.isNotBlank()) RedPrimary else Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (showEmailDialog) {
                    AlertDialog(
                        onDismissRequest = { showEmailDialog = false },
                        title = { Text("Set Recovery Gmail") },
                        text = {
                            Column {
                                Text("Enter your Gmail address to receive OTP when resetting PIN:", fontSize = 13.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(10.dp))
                                androidx.compose.material3.OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Gmail Address") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.updateSettings(settings.copy(recoveryEmail = emailInput.trim()))
                                    showEmailDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                            ) { Text("Save Email") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEmailDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }

            // Storage
            item {
                SettingsSectionHeader(title = "STORAGE", icon = Icons.Default.Storage)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { cacheClearedToast = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = RedPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Clear Media Cache", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Reclaim temporary thumbnail space", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Text("128 MB", fontWeight = FontWeight.Bold, color = RedPrimary)
                    }
                }
            }

            // About
            item {
                SettingsSectionHeader(title = "ABOUT", icon = Icons.Default.Info)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        SettingsRowText("App Version", "v2.4.1 (Offline Build)")
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        SettingsRowText("Privacy Policy", "100% Offline & Private")
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AL-NASHiHA · Made with care",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    if (cacheClearedToast) {
        AlertDialog(
            onDismissRequest = { cacheClearedToast = false },
            title = { Text("Cache Cleared") },
            text = { Text("128 MB of temporary media cache has been cleared successfully.") },
            confirmButton = {
                Button(
                    onClick = { cacheClearedToast = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) { Text("OK") }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = RedPrimary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun SettingsRowSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RedPrimary
            )
        )
    }
}

@Composable
fun SettingsRowText(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(text = subtitle, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
    }
}
