package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.database.AppDatabase
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.TouchGrassRepository
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TouchGrassApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var repository: TouchGrassRepository
        private set
    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        repository = TouchGrassRepository(database)
        preferencesManager = PreferencesManager(this)
        createNotificationChannels()

        // Pre-create WebView cache directories to prevent Chromium disk cache warnings
        try {
            java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js").mkdirs()
            java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm").mkdirs()
        } catch (_: Throwable) {}

        CoroutineScope(Dispatchers.IO).launch {
            repository.initializeBadges()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                enableVibration(true)
            }
            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR_ID,
                getString(R.string.monitoring_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.monitoring_service_channel_desc)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(alertChannel)
            notificationManager.createNotificationChannel(monitorChannel)
        }
    }

    companion object {
        const val CHANNEL_ALERTS_ID = "touch_grass_alerts_channel"
        const val CHANNEL_MONITOR_ID = "touch_grass_monitor_channel"
        lateinit var instance: TouchGrassApp
            private set
    }
}
