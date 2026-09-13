package com.example.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.data.database.entity.AppUsageLimit
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

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    override fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun getUsageAccessSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    private fun getStartOfDayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    override suspend fun getTodayTotalScreenTimeMillis(): Long = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission() || usageStatsManager == null) return@withContext 0L
        val start = getStartOfDayMillis()
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now) ?: emptyList()
        stats.sumOf { it.totalTimeInForeground }
    }

    override suspend fun getInstalledInteractiveApps(): List<AppUsageInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)

        val popularScrollingPackages = setOf(
            "com.google.android.youtube",
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.reddit.frontpage",
            "com.twitter.android",
            "com.facebook.katana",
            "com.snapchat.android",
            "com.pinterest",
            "com.google.android.apps.tachyon",
            "com.netflix.mediaclient",
            "com.discord"
        )

        val seen = mutableSetOf<String>()
        val result = mutableListOf<AppUsageInfo>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName || seen.contains(pkg)) continue
            seen.add(pkg)

            val label = resolveInfo.loadLabel(pm).toString()
            val icon = resolveInfo.loadIcon(pm)
            val isKnownScrollingApp = popularScrollingPackages.contains(pkg)

            result.add(
                AppUsageInfo(
                    packageName = pkg,
                    appName = label,
                    dailyUsageMillis = 0L,
                    dailyLimitMinutes = 30,
                    isMonitored = isKnownScrollingApp,
                    icon = icon
                )
            )
        }

        result.sortedWith(
            compareByDescending<AppUsageInfo> { it.isMonitored }
                .thenBy { it.appName.lowercase() }
        )
    }

    override suspend fun getDailyAppUsageList(): List<AppUsageInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedApps = getInstalledInteractiveApps().associateBy { it.packageName }
        val usageMap = mutableMapOf<String, Long>()

        if (hasUsageStatsPermission() && usageStatsManager != null) {
            val start = getStartOfDayMillis()
            val now = System.currentTimeMillis()
            val statsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now) ?: emptyList()
            for (stat in statsList) {
                if (stat.totalTimeInForeground > 0) {
                    val current = usageMap[stat.packageName] ?: 0L
                    usageMap[stat.packageName] = maxOf(current, stat.totalTimeInForeground)
                }
            }
        }

        installedApps.values.map { app ->
            val usageMillis = usageMap[app.packageName] ?: 0L
            app.copy(dailyUsageMillis = usageMillis)
        }.sortedByDescending { it.dailyUsageMillis }
    }

    override fun observeMonitoredAppsWithUsage(): Flow<List<AppUsageInfo>> {
        return combine(
            repository.allLimits,
            flow {
                while (true) {
                    emit(getDailyAppUsageList())
                    kotlinx.coroutines.delay(5000) // Poll every 5s for fresh real usage
                }
            }
        ) { limits, usageList ->
            val limitMap = limits.associateBy { it.packageName }
            usageList.map { app ->
                val savedLimit = limitMap[app.packageName]
                if (savedLimit != null) {
                    app.copy(
                        isMonitored = savedLimit.isMonitored,
                        dailyLimitMinutes = savedLimit.customLimitMinutes
                    )
                } else {
                    app
                }
            }
        }.flowOn(Dispatchers.IO)
    }
}
