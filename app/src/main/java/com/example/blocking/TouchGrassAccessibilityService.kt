package com.example.blocking

import android.app.usage.UsageStatsManager
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.TouchGrassApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Watches foreground app changes only after the user has explicitly enabled this service.
 * A monitored app is interrupted only after its configured daily UsageStats limit is reached
 * and any rewarded-ad grace period for that package has expired.
 */
class TouchGrassAccessibilityService : AccessibilityService() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var bypassManager: AdBypassManager
    private val lastInterventionByPackage = mutableMapOf<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        bypassManager = AdBypassManager(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return

        serviceScope.launch {
            val app = application as? TouchGrassApp ?: return@launch
            val limit = app.repository.getAppUsageLimit(packageName) ?: return@launch
            if (!limit.isMonitored || limit.customLimitMinutes <= 0) return@launch
            if (bypassManager.isActive(packageName)) return@launch

            val usageMinutes = getTodayForegroundMinutes(packageName)
            if (usageMinutes < limit.customLimitMinutes) return@launch

            val now = System.currentTimeMillis()
            val last = lastInterventionByPackage[packageName] ?: 0L
            if (now - last < 20_000L) return@launch
            lastInterventionByPackage[packageName] = now

            val intent = Intent(applicationContext, InterventionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, packageName)
                putExtra(InterventionActivity.EXTRA_APP_NAME, limit.appName)
                putExtra(InterventionActivity.EXTRA_SCROLLING_MINUTES, usageMinutes)
            }
            startActivity(intent)
        }
    }

    private fun getTodayForegroundMinutes(packageName: String): Int {
        val manager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val stats = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            calendar.timeInMillis,
            System.currentTimeMillis()
        ) ?: return 0
        val millis = stats.filter { it.packageName == packageName }.sumOf { it.totalTimeInForeground }
        return (millis / 60_000L).toInt()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
