package com.example.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.data.repository.TouchGrassRepository
import com.example.domain.model.AppUsageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Calendar

class AndroidUsageMonitor(
    private val context: Context,
    private val repository: TouchGrassRepository
) : UsageMonitor {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    override fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun getUsageAccessSettingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    private fun getStartOfDayMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun queryTodayStats() = usageStatsManager?.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        getStartOfDayMillis(),
        System.currentTimeMillis()
    ) ?: emptyList()

    override suspend fun getTodayTotalScreenTimeMillis(): Long = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission() || usageStatsManager == null) return@withContext 0L
        queryTodayStats().sumOf { it.totalTimeInForeground }
    }

    override suspend fun getInstalledInteractiveApps(): List<AppUsageInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)

        val seen = mutableSetOf<String>()
        val result = mutableListOf<AppUsageInfo>()
        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName || !seen.add(pkg)) continue
            result += AppUsageInfo(
                packageName = pkg,
                appName = resolveInfo.loadLabel(pm).toString(),
                dailyUsageMillis = 0L,
                dailyLimitMinutes = 30,
                isMonitored = false,
                icon = resolveInfo.loadIcon(pm)
            )
        }
        result.sortedBy { it.appName.lowercase() }
    }

    override suspend fun getDailyAppUsageList(): List<AppUsageInfo> = withContext(Dispatchers.IO) {
        val installedApps = getInstalledInteractiveApps().associateBy { it.packageName }
        val usageMap = mutableMapOf<String, Long>()

        if (hasUsageStatsPermission() && usageStatsManager != null) {
            // queryUsageStats can return multiple buckets for the same package. Sum them;
            // taking only the maximum bucket under-reports screen time on some Android versions.
            for (stat in queryTodayStats()) {
                if (stat.totalTimeInForeground > 0) {
                    usageMap[stat.packageName] = (usageMap[stat.packageName] ?: 0L) + stat.totalTimeInForeground
                }
            }
        }

        installedApps.values.map { app ->
            app.copy(dailyUsageMillis = usageMap[app.packageName] ?: 0L)
        }.sortedByDescending { it.dailyUsageMillis }
    }

    override fun observeMonitoredAppsWithUsage(): Flow<List<AppUsageInfo>> = combine(
        repository.allLimits,
        flow {
            while (true) {
                emit(getDailyAppUsageList())
                kotlinx.coroutines.delay(5_000L)
            }
        }
    ) { limits, usageList ->
        val limitMap = limits.associateBy { it.packageName }
        usageList.map { app ->
            val savedLimit = limitMap[app.packageName]
            if (savedLimit != null) {
                app.copy(isMonitored = savedLimit.isMonitored, dailyLimitMinutes = savedLimit.customLimitMinutes)
            } else app
        }
    }.flowOn(Dispatchers.IO)
}
