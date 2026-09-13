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
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14291B))
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Strict Phone Lock", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                                Text("Protect every normal app after your daily limit.", color = Color(0xFFA5D6A7), fontSize = 12.sp)
                            }
                            Switch(
                                checked = state.settings.strictLockEnabled,
                                onCheckedChange = viewModel::toggleStrictLock,
                                colors = SwitchDefaults.colors(checkedThumbColor = GrassGreenPrimary, checkedTrackColor = GrassGreenPrimary.copy(alpha = 0.35f)),
                                modifier = Modifier.testTag("strict_lock_switch")
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "ON = Instagram, YouTube, X, Chrome, games and other normal apps are covered automatically. You do not need to add apps one by one. Phone/dialer calls remain available.",
                            color = Color(0xFFE8F5E9), fontSize = 12.sp, lineHeight = 17.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Text("Global daily limit: ${state.settings.globalDailyLimitMinutes} minutes", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf(15, 30, 45, 60, 90, 120).forEach { mins ->
                                FilterChip(
                                    selected = state.settings.globalDailyLimitMinutes == mins,
                                    onClick = { viewModel.setGlobalLimit(mins) },
                                    label = { Text("${mins}m") },
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

            item {
                Text("Android Intervention Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
            item {
                PermissionSettingItem("Usage Access", "Calculates real daily foreground usage across apps.", state.hasUsageStatsPermission) {
                    context.startActivity(viewModel.usageMonitor.getUsageAccessSettingsIntent())
                }
            }
            item {
                PermissionSettingItem("Intervention Guardian (Accessibility)", "Notices foreground-app changes so the global lock can intervene. It does not read messages or passwords.", state.isAccessibilityEnabled) {
                    showAccessibilityDisclosure = true
                }
            }
            item {
                PermissionSettingItem("Display Over Other Apps", "Lets the lock screen appear immediately above another app.", state.canDrawOverlays) {
                    context.startActivity(viewModel.blockingManager.getOverlaySettingsIntent())
                }
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
                        Text("Keep real grass contact continuously for the selected duration.", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3, 5, 8).forEach { sec ->
                                FilterChip(
                                    selected = state.settings.holdDurationSeconds == sec,
                                    onClick = { viewModel.setHoldDuration(sec) },
                                    label = { Text("${sec}s Hold") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GrassGreenPrimary.copy(alpha = 0.2f), selectedLabelColor = GrassGreenPrimary)
                                )
                            }
                        }
                    }
                }
            }
            item { SettingToggleItem("Funny & Gen-Z Quotes", "Roasts for blocks, failed cheats and successful grass touches.", state.settings.funnyMessagesEnabled, viewModel::toggleFunnyMessages) }
            item { SettingToggleItem("Sound & Vibration Haptics", "Tactile countdown and completion feedback.", state.settings.soundVibrationEnabled, viewModel::toggleSoundVibration) }
            item { SettingToggleItem("Screen Time Notifications", "Reminders when your daily screen-time limit is getting close.", state.settings.notificationsEnabled, viewModel::toggleNotifications) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14291B))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Local Camera Privacy", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE8F5E9))
                            Text("Verification runs on-device. Camera frames are analyzed in memory and are not saved or uploaded by Touch Grass.", fontSize = 12.sp, color = Color(0xFFA5D6A7), lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
            item {
                TextButton(onClick = { showResetDialog = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    Text("Reset All Streaks & Data", color = Color(0xFFE57373), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (showAccessibilityDisclosure) {
            AlertDialog(
                onDismissRequest = { showAccessibilityDisclosure = false },
                title = { Text("Accessibility permission") },
                text = { Text("Touch Grass uses Accessibility to notice which app is in the foreground so the global screen-time intervention can appear. It does not read passwords, messages, or page content. Android requires this permission for foreground-app intervention.") },
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
                confirmButton = { TextButton(onClick = { viewModel.resetStatistics(); showResetDialog = false }) { Text("Reset", color = Color.Red) } },
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
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (isGranted) GrassGreenPrimary.copy(alpha = 0.15f) else Color(0xFFFFB74D).copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
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
