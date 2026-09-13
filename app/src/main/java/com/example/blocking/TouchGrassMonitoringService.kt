package com.example.blocking

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.TouchGrassApp
import com.example.notifications.NotificationHelper
import com.example.usage.AndroidUsageMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TouchGrassMonitoringService : Service() {
    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var usageMonitor: AndroidUsageMonitor
    private val recentlyAlertedPackages = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        val app = application as TouchGrassApp
        usageMonitor = AndroidUsageMonitor(this, app.repository)

        val notification = notificationHelper.buildMonitoringForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        startMonitoringLoop()
    }

    private fun startMonitoringLoop() {
        scope.launch {
            while (isActive) {
                try {
                    checkUsageAndNotify()
                } catch (t: Throwable) {
                    Log.e(TAG, "Usage monitoring iteration failed", t)
                }
                delay(15_000L)
            }
        }
    }

    private suspend fun checkUsageAndNotify() {
        if (!usageMonitor.hasUsageStatsPermission()) return
        val app = application as TouchGrassApp
        val monitoredUsage = usageMonitor.getDailyAppUsageList()
        val now = System.currentTimeMillis()

        for (item in monitoredUsage) {
            val limit = app.repository.getAppUsageLimit(item.packageName) ?: continue
            if (!limit.isMonitored || limit.customLimitMinutes <= 0) continue
            if (item.dailyUsageMinutes < limit.customLimitMinutes) continue

            val lastAlert = recentlyAlertedPackages[item.packageName] ?: 0L
            if (now - lastAlert > 10 * 60 * 1000L) {
                recentlyAlertedPackages[item.packageName] = now
                notificationHelper.showLimitReachedNotification(
                    packageName = item.packageName,
                    appName = item.appName,
                    minutes = item.dailyUsageMinutes
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 2002
        private const val TAG = "TouchGrassMonitor"
    }
}
