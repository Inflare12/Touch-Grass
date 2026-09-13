package com.example.blocking

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
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

    // Memory cache of alerted packages today to prevent spamming
    private val recentlyAlertedPackages = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        val app = application as TouchGrassApp
        usageMonitor = AndroidUsageMonitor(this, app.repository)

        val notification = notificationHelper.buildMonitoringForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startMonitoringLoop()
    }

    private fun startMonitoringLoop() {
        scope.launch {
            while (isActive) {
                try {
                    checkUsageAndIntervene()
                } catch (_: Exception) {}
                delay(15000) // Efficient 15s poll
            }
        }
    }

    private suspend fun checkUsageAndIntervene() {
        if (!usageMonitor.hasUsageStatsPermission()) return

        val app = application as TouchGrassApp
        val monitoredUsage = usageMonitor.getDailyAppUsageList()
        val now = System.currentTimeMillis()

        for (item in monitoredUsage) {
            val limit = app.repository.getAppUsageLimit(item.packageName)
            if (limit != null && limit.isMonitored) {
                if (item.dailyUsageMinutes >= limit.customLimitMinutes) {
                    val lastAlert = recentlyAlertedPackages[item.packageName] ?: 0L
                    // Alert at most once every 10 minutes unless cleared
                    if (now - lastAlert > 10 * 60 * 1000) {
                        recentlyAlertedPackages[item.packageName] = now
                        notificationHelper.showLimitReachedNotification(
                            packageName = item.packageName,
                            appName = item.appName,
                            minutes = item.dailyUsageMinutes
                        )
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 2002
    }
}
