package com.example.ui.home

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.components.MetricStatCard
import com.example.ui.theme.GrassGreenPrimary
import com.example.ui.viewmodel.TouchGrassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: TouchGrassViewModel, onNavigateToChallenge: () -> Unit, onNavigateToApps: () -> Unit, onNavigateToStats: () -> Unit, onNavigateToSettings: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val totalHours = (state.totalScreenTimeMillis / (1000 * 60 * 60)).toInt()
    val totalMinutes = ((state.totalScreenTimeMillis / (1000 * 60)) % 60).toInt()
    val formattedScreenTime = if (totalHours > 0) "${totalHours}h ${totalMinutes}m" else "${totalMinutes}m"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Text("🌱", fontSize = 24.sp); Spacer(Modifier.width(8.dp)); Text("Touch Grass", fontWeight = FontWeight.Bold) } },
                actions = {
                    IconButton(onClick = onNavigateToStats, modifier = Modifier.testTag("home_stats_button")) { Icon(Icons.Default.BarChart, contentDescription = "Statistics") }
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.testTag("home_settings_button")) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("home_screen")
    ) { paddingValues ->
        LazyColumn(Modifier.fillMaxSize().padding(paddingValues), contentPadding = PaddingValues(bottom = 32.dp)) {
            if (!state.hasUsageStatsPermission) {
                item {
                    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clickable { context.startActivity(viewModel.usageMonitor.getUsageAccessSettingsIntent()) }.testTag("usage_permission_banner"),
                        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF332015))) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Enable Usage Access", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFFFE0B2))
                                Text("Tap to grant permission so Touch Grass can detect scrolling app time.", fontSize = 12.sp, color = Color(0xFFFFCC80))
                            }
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))) {
                    Column(Modifier.padding(20.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("TODAY'S SCREEN TIME", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("${state.monitoredApps.size} monitored", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(formattedScreenTime, fontSize = 42.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(4.dp))
                        Text("Grass touched today: ${state.streak.grassTouchedTodayCount} times", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }

            item {
                Button(onClick = onNavigateToChallenge, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).height(64.dp).testTag("home_touch_grass_now_button"), colors = ButtonDefaults.buttonColors(containerColor = GrassGreenPrimary), shape = RoundedCornerShape(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Text("🌱", fontSize = 24.sp); Spacer(Modifier.width(10.dp)); Text("Touch Grass Now", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                }
            }

            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricStatCard(Modifier.weight(1f), "Grass Streak", "${state.streak.currentStreak} days", "🔥", onNavigateToStats)
                        MetricStatCard(Modifier.weight(1f), "Challenges", "${state.streak.totalChallengesCompleted}", "🌱", onNavigateToStats)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricStatCard(Modifier.weight(1f), "Bypasses Used", "${state.streak.totalBypassesUsed}", "📺", onNavigateToStats)
                        MetricStatCard(Modifier.weight(1f), "Grass Touches", "${state.streak.grassTouchedTodayCount}", "🖐️", onNavigateToStats)
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Monitored Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(onClick = onNavigateToApps, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.testTag("manage_apps_button")) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Manage", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }

            if (state.monitoredApps.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📱", fontSize = 36.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("No apps monitored yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Select apps like YouTube, Instagram, TikTok, or Reddit to set your daily limits.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            } else {
                items(state.monitoredApps, key = { it.packageName }) { appInfo -> AppUsageCard(appInfo, onNavigateToApps) }
            }
        }
    }
}

@Composable
fun AppUsageCard(appInfo: AppUsageInfo, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).clickable { onClick() }.testTag("app_usage_card_${appInfo.packageName}"), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(GrassGreenPrimary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text(appInfo.appName.take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GrassGreenPrimary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(appInfo.appName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("${appInfo.dailyUsageMinutes}m / ${appInfo.dailyLimitMinutes}m", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (appInfo.isLimitExceeded) Color(0xFFE57373) else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { appInfo.progressRatio }, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = if (appInfo.isLimitExceeded) Color(0xFFEF5350) else GrassGreenPrimary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}
