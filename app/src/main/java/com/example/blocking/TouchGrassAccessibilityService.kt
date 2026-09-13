package com.example.blocking

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.TouchGrassApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Enforces the global lock after the daily screen-time limit. Normal user apps are
 * interrupted while the lock is active; phone/dialer packages remain available.
 * The lock is persisted so switching apps, Home, or recents does not reset it.
 */
class TouchGrassAccessibilityService : AccessibilityService() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var bypassManager: AdBypassManager
    private var lastInterventionAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        bypassManager = AdBypassManager(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        val app = application as? TouchGrassApp ?: return
        if (packageName == applicationContext.packageName || isAllowedCallPackage(packageName)) return

        serviceScope.launch {
            val settings = app.preferencesManager.settingsFlow.firstOrNull() ?: return@launch
            if (!settings.strictLockEnabled) return@launch
            if (bypassManager.isGlobalActive()) return@launch

            val totalMinutes = getTodayTotalForegroundMinutes()
            val limitReached = totalMinutes >= settings.globalDailyLimitMinutes
            if (!settings.globalLockActive && limitReached) app.preferencesManager.setGlobalLockActive(true)

            val locked = settings.globalLockActive || limitReached
            if (!locked || bypassManager.isActive(packageName)) return@launch

            val now = System.currentTimeMillis()
            if (now - lastInterventionAt < 1_500L) return@launch
            lastInterventionAt = now

            val intent = Intent(applicationContext, InterventionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, packageName)
                putExtra(InterventionActivity.EXTRA_APP_NAME, "Your phone")
                putExtra(InterventionActivity.EXTRA_SCROLLING_MINUTES, totalMinutes)
                putExtra(InterventionActivity.EXTRA_GLOBAL_LOCK, true)
            }
            startActivity(intent)
        }
    }

    private fun getTodayTotalForegroundMinutes(): Int {
        val manager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, calendar.timeInMillis, System.currentTimeMillis()) ?: return 0
        return (stats.filter { it.packageName != applicationContext.packageName }.sumOf { it.totalTimeInForeground } / 60_000L).toInt()
    }

    private fun isAllowedCallPackage(packageName: String): Boolean {
        val p = packageName.lowercase()
        return p.contains("dialer") || p.contains("incallui") ||
            p == "com.android.phone" || p == "com.android.server.telecom" ||
            p == "com.google.android.dialer" || p == "com.samsung.android.dialer"
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
