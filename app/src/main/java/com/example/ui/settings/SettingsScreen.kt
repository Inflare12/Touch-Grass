package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.theme.GrassGreenPrimary
import com.example.ui.viewmodel.TouchGrassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TouchGrassViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshPermissions() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Rules", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("settings_screen")
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    "Android Intervention Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            item {
                PermissionSettingItem(
                    title = "Usage Access",
                    subtitle = "Required to calculate real daily app usage and detect scrolling.",
                    isGranted = state.hasUsageStatsPermission,
                    onClick = { context.startActivity(viewModel.usageMonitor.getUsageAccessSettingsIntent()) }
                )
            }
            item {
                PermissionSettingItem(
                    title = "Intervention Guardian (Accessibility)",
                    subtitle = "Detects selected app launches after their configured usage limit. Touch Grass does not read window content.",
                    isGranted = state.isAccessibilityEnabled,
                    onClick = { showAccessibilityDisclosure = true }
                )
            }
            item {
                PermissionSettingItem(
                    title = "Display Over Other Apps",
                    subtitle = "Allows Touch Grass to show the lock intervention above scrolling apps.",
                    isGranted = state.canDrawOverlays,
                    onClick = { context.startActivity(viewModel.blockingManager.getOverlaySettingsIntent()) }
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text("Challenge Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Grass Contact Hold Duration", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("How many seconds you must continuously maintain physical contact with grass.", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3, 5, 8).forEach { sec ->
                                FilterChip(
                                    selected = state.settings.holdDurationSeconds == sec,
                                    onClick = { viewModel.setHoldDuration(sec) },
                                    label = { Text("${sec}s Hold") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GrassGreenPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = GrassGreenPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
            item { SettingToggleItem("Funny & Humorous Quotes", "Enable witty banter when blocked or attempting to cheat.", state.settings.funnyMessagesEnabled, viewModel::toggleFunnyMessages) }
            item { SettingToggleItem("Sound & Vibration Haptics", "Tactile countdown and completion feedback.", state.settings.soundVibrationEnabled, viewModel::toggleSoundVibration) }
            item { SettingToggleItem("Screen Time Notifications", "Receive reminders when apps hit their daily threshold.", state.settings.notificationsEnabled, viewModel::toggleNotifications) }

            item {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14291B))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Local Camera Privacy Guarantee", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE8F5E9))
                            Text("Verification runs on-device. Camera frames are analyzed in memory and are not saved or uploaded by Touch Grass.",
                                fontSize = 12.sp, color = Color(0xFFA5D6A7), lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = { showResetDialog = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Text("Reset All Streaks & Data", color = Color(0xFFE57373), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (showAccessibilityDisclosure) {
            AlertDialog(
                onDismissRequest = { showAccessibilityDisclosure = false },
                title = { Text("Accessibility permission") },
                text = {
                    Text("Touch Grass uses Accessibility only to notice when a selected app becomes the foreground app after its daily usage limit is exceeded. It does not read window text, messages, passwords, or page content. Android requires this permission because there is no ordinary app API for this foreground-app intervention flow.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        showAccessibilityDisclosure = false
                        context.startActivity(viewModel.blockingManager.getAccessibilitySettingsIntent())
                    }) { Text("I understand") }
                },
                dismissButton = { TextButton(onClick = { showAccessibilityDisclosure = false }) { Text("Cancel") } }
            )
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset All Data?") },
                text = { Text("This clears streaks, challenge records, and custom settings.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetStatistics(); showResetDialog = false }) { Text("Reset", color = Color.Red) }
                },
                dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun PermissionSettingItem(title: String, subtitle: String, isGranted: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null,
                tint = if (isGranted) GrassGreenPrimary else Color(0xFFFFB74D), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(
                if (isGranted) GrassGreenPrimary.copy(alpha = 0.15f) else Color(0xFFFFB74D).copy(alpha = 0.15f)
            ).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(if (isGranted) "Granted" else "Tap to Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = if (isGranted) GrassGreenPrimary else Color(0xFFFFB74D))
            }
        }
    }
}

@Composable
fun SettingToggleItem(title: String, subtitle: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = isChecked, onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = GrassGreenPrimary, checkedTrackColor = GrassGreenPrimary.copy(alpha = 0.3f)))
        }
    }
}
