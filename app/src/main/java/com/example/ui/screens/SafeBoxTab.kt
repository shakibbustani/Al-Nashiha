package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.model.AppSettingsEntity
import com.example.ui.MainViewModel
import com.example.ui.components.MediaCard
import com.example.ui.theme.RedLight
import com.example.ui.theme.RedPrimary
import kotlin.random.Random

@Composable
fun SafeBoxTab(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val isUnlocked by viewModel.safeBoxUnlocked.collectAsState()
    val lockedMedia by viewModel.lockedMedia.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currentPin = settings?.safeBoxPin?.ifEmpty { "1234" } ?: "1234"
    val isFingerprintEnabled = settings?.fingerprintEnabled ?: false

    val currentRecoveryKey = settings?.recoveryKey ?: ""

    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showForgotPinDialog by remember { mutableStateOf(false) }
    var showResetSuccessDialog by remember { mutableStateOf(false) }
    var newKeyAfterReset by remember { mutableStateOf("") }

    // Helper to prompt for biometric hardware authentication
    fun handleBiometricClick() {
        if (!isFingerprintEnabled) {
            Toast.makeText(
                context,
                "Fingerprint Lock is OFF in Settings! Please enable it in Settings -> Safe Box Security.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val activity = context as? FragmentActivity
        if (activity == null) {
            Toast.makeText(context, "Activity error for fingerprint prompt", Toast.LENGTH_SHORT).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.unlockWithFingerprint()
                    Toast.makeText(context, "Fingerprint Verified! Safe Box Unlocked.", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(context, "Biometric error: $errString", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(context, "Fingerprint not recognized. Access Denied!", Toast.LENGTH_SHORT).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Safe Box Fingerprint Authentication")
            .setSubtitle("Place your finger on the scanner to unlock")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Toast.makeText(context, "Biometric sensor error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    if (!isUnlocked) {
        // PIN ENTRY SCREEN
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Lock Icon in circular light-red container
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(RedLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Safe Box Lock",
                    tint = RedPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Enter Safe Box PIN",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("pin_title")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isFingerprintEnabled) "Enter 4-digit PIN or use Fingerprint" else "Enter 4-digit PIN to unlock",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Underline PIN Boxes
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < pinInput.length
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isFilled) RedPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
                            .border(
                                width = 2.dp,
                                color = if (pinError) RedPrimary else if (isFilled) RedPrimary else Color.LightGray,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFilled) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(RedPrimary)
                            )
                        }
                    }
                }
            }

            if (pinError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Incorrect PIN code. Please try again.",
                    color = RedPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Numeric Keypad Grid with Fingerprint Button
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("FP", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keys.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        row.forEach { key ->
                            Surface(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (key == "FP") {
                                            handleBiometricClick()
                                        } else if (key == "DEL") {
                                            if (pinInput.isNotEmpty()) {
                                                pinInput = pinInput.dropLast(1)
                                                pinError = false
                                            }
                                        } else if (pinInput.length < 4) {
                                            val newInput = pinInput + key
                                            pinInput = newInput
                                            pinError = false
                                            if (newInput.length == 4) {
                                                val success = viewModel.verifyPin(newInput, currentPin)
                                                if (!success) {
                                                    pinError = true
                                                    pinInput = ""
                                                }
                                            }
                                        }
                                    }
                                    .testTag("keypad_$key"),
                                color = if (key == "FP") {
                                    if (isFingerprintEnabled) RedLight else Color.LightGray.copy(alpha = 0.2f)
                                } else MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp,
                                shape = CircleShape
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "FP") {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Fingerprint Unlock",
                                            tint = if (isFingerprintEnabled) RedPrimary else Color.Gray,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else if (key == "DEL") {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete PIN Digit",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = { handleBiometricClick() },
                    modifier = Modifier.testTag("fingerprint_unlock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = if (isFingerprintEnabled) RedPrimary else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Fingerprint Unlock",
                        color = if (isFingerprintEnabled) RedPrimary else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                TextButton(
                    onClick = { showForgotPinDialog = true },
                    modifier = Modifier.testTag("forgot_pin_button")
                ) {
                    Text("Forgot PIN?", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
    } else {
        // UNLOCKED SAFE BOX CONTENT
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header: Safe Box (count) + Change PIN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Safe Box (${lockedMedia.size})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedButton(
                    onClick = { showChangePinDialog = true },
                    border = BorderStroke(1.dp, RedPrimary),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = "Change PIN",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change PIN", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Light-red info banner
            Card(
                colors = CardDefaults.cardColors(containerColor = RedLight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = RedPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "These contents are PIN & Fingerprint protected — only you can view them.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "LOCKED MEDIA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = RedPrimary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (lockedMedia.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No locked files in Safe Box.\nUse 3-dots on any file in Media tab to lock it here.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(lockedMedia, key = { it.id }) { item ->
                        MediaCard(
                            item = item,
                            onClick = { viewModel.playMedia(item) },
                            onRename = { /* rename */ },
                            onMoveToFolder = {},
                            onAddToPlaylist = {},
                            onLockToggle = { viewModel.unlockMedia(item) },
                            onDelete = { viewModel.deleteMedia(item) },
                            onShare = {}
                        )
                    }
                }
            }
        }
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Set New Safe Box PIN") },
            text = {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) newPin = it },
                    label = { Text("4-Digit PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length == 4) {
                            val updated = settings?.copy(safeBoxPin = newPin)
                                ?: AppSettingsEntity(safeBoxPin = newPin)
                            viewModel.updateSettings(updated)
                            showChangePinDialog = false
                            Toast.makeText(context, "New PIN saved successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter exactly 4 digits", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) { Text("Save PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Forgot PIN Offline Recovery Key Dialog
    if (showForgotPinDialog) {
        var keyInput by remember { mutableStateOf("") }
        var keyErrorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showForgotPinDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockReset, contentDescription = null, tint = RedPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Forgot Safe Box PIN")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter your single-use 16-character Recovery Key (e.g. A7K9-X2M4-P8Q1-Z5R3) to reset your PIN to default (1234):",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = it
                            keyErrorMsg = null
                        },
                        label = { Text("Recovery Key / Backup Code") },
                        placeholder = { Text("A7K9-X2M4-P8Q1-Z5R3") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (keyErrorMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = keyErrorMsg!!, color = RedPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val activeDbKey = settings?.recoveryKey ?: ""
                        val normalizedInput = com.example.util.normalizeKey(keyInput)
                        val normalizedActiveKey = com.example.util.normalizeKey(activeDbKey)

                        if (normalizedInput.isNotEmpty() && normalizedActiveKey.isNotEmpty() && normalizedInput == normalizedActiveKey) {
                            // Valid Recovery Key! Reset PIN to default 1234 & auto-generate NEW Recovery Key
                            val brandNewKey = com.example.util.generateRecoveryKey()
                            val resetSettings = (settings ?: AppSettingsEntity()).copy(
                                safeBoxPin = "1234",
                                recoveryKey = brandNewKey
                            )
                            viewModel.updateSettings(resetSettings)

                            // Save new key as PDF automatically
                            val newPdf = com.example.util.createRecoveryKeyPdf(context, brandNewKey)
                            newKeyAfterReset = newPdf?.absolutePath ?: "Downloads/AL-NASHiHA_Recovery_Key.pdf"

                            showForgotPinDialog = false
                            showResetSuccessDialog = true
                        } else {
                            keyErrorMsg = "Invalid Recovery Key! Please check the key inside your saved backup PDF."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Verify & Reset PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success Dialog after PIN Reset displaying PDF save location
    if (showResetSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showResetSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = RedPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PIN Reset Successful!")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Your Safe Box PIN has been reset to default: 1234",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "IMPORTANT: Because your previous key was used, a NEW single-use Recovery Key PDF has been generated and saved for you:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = RedLight),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "New PDF Location:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(text = newKeyAfterReset, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Please keep this PDF file safe in case you forget your PIN again in the future.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showResetSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
