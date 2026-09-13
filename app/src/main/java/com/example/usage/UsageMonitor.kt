package com.example.usage

import android.content.Intent
import com.example.domain.model.AppUsageInfo
import kotlinx.coroutines.flow.Flow

interface UsageMonitor {
    fun hasUsageStatsPermission(): Boolean
    fun getUsageAccessSettingsIntent(): Intent
    suspend fun getTodayTotalScreenTimeMillis(): Long
    suspend fun getInstalledInteractiveApps(): List<AppUsageInfo>
    suspend fun getDailyAppUsageList(): List<AppUsageInfo>
    fun observeMonitoredAppsWithUsage(): Flow<List<AppUsageInfo>>
}
