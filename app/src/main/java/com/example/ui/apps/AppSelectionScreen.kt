package com.example.ui.apps

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.domain.model.AppUsageInfo
import com.example.ui.theme.GrassGreenPrimary
import com.example.ui.viewmodel.TouchGrassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(viewModel: TouchGrassViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = state.allInstalledApps.filter { it.appName.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Coverage", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = { IconButton(onClick = onBack, modifier = Modifier.testTag("apps_back_button")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("app_selection_screen")
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
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("All-app protection is automatic", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                            Text(
                                if (state.settings.strictLockEnabled) "Strict Lock is ON: every normal launchable app is covered after the global limit. No adding Instagram, X, YouTube, Chrome or games one-by-one."
                                else "Strict Lock is OFF: use the switches below for individual app limits.",
                                color = Color(0xFFA5D6A7), fontSize = 12.sp, lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).testTag("app_search_field"),
                    placeholder = { Text("Search installed apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
            if (!state.hasUsageStatsPermission) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clickable { context.startActivity(viewModel.usageMonitor.getUsageAccessSettingsIntent()) },
                        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF332015))
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Usage Access is required for real daily screen-time totals. Tap to grant it.", color = Color(0xFFFFCC80), fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
            items(filteredApps, key = { it.packageName }) { appInfo ->
                AppManageItem(
                    appInfo = appInfo,
                    strictLockEnabled = state.settings.strictLockEnabled,
                    onToggleMonitored = { monitored -> viewModel.toggleAppMonitored(appInfo.packageName, appInfo.appName, monitored) },
                    onLimitChanged = { minutes -> viewModel.setAppLimitMinutes(appInfo.packageName, appInfo.appName, minutes) }
                )
            }
        }
    }
}

@Composable
fun AppManageItem(
    appInfo: AppUsageInfo,
    strictLockEnabled: Boolean = false,
    onToggleMonitored: (Boolean) -> Unit,
    onLimitChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).testTag("app_manage_card_${appInfo.packageName}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (strictLockEnabled) 0.35f else 0.6f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(GrassGreenPrimary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) { Text(appInfo.appName.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GrassGreenPrimary) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(appInfo.appName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Used today: ${appInfo.formattedUsage}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (strictLockEnabled) {
                    Text("PROTECTED", fontSize = 10.sp, fontWeight = FontWeight.Black, color = GrassGreenPrimary,
                        modifier = Modifier.background(GrassGreenPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 5.dp))
                } else {
                    Switch(
                        checked = appInfo.isMonitored,
                        onCheckedChange = onToggleMonitored,
                        colors = SwitchDefaults.colors(checkedThumbColor = GrassGreenPrimary, checkedTrackColor = GrassGreenPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.testTag("switch_${appInfo.packageName}")
                    )
                }
            }
            if (!strictLockEnabled && appInfo.isMonitored) {
                Spacer(Modifier.height(12.dp))
                Text("Daily Limit: ${appInfo.dailyLimitMinutes} mins", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val limitOptions = listOf(15, 30, 45, 60, 90, 120)
                    items(limitOptions) { mins ->
                        val isSelected = appInfo.dailyLimitMinutes == mins
                        FilterChip(
                            selected = isSelected,
                            onClick = { onLimitChanged(mins) },
                            label = { Text("${mins}m") },
                            leadingIcon = if (isSelected) ({ Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }) else null,
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GrassGreenPrimary.copy(alpha = 0.2f), selectedLabelColor = GrassGreenPrimary)
                        )
                    }
                }
            }
        }
    }
}
