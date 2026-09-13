package com.example.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.TouchGrassApp
import com.example.blocking.InterventionActivity

class NotificationHelper(private val context: Context) {

    fun buildMonitoringForegroundNotification(): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, TouchGrassApp.CHANNEL_MONITOR_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Touch Grass Guardian Active")
            .setContentText("Monitoring screen time. Grass is waiting outside.")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun showLimitReachedNotification(packageName: String, appName: String, minutes: Int) {
        val interventionIntent = Intent(context, InterventionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(InterventionActivity.EXTRA_APP_NAME, appName)
            putExtra(InterventionActivity.EXTRA_SCROLLING_MINUTES, minutes)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            packageName.hashCode(),
            interventionIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, TouchGrassApp.CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔒 Time's Up on $appName")
            .setContentText("You reached your limit of $minutes minutes. Time to touch grass!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You reached your scrolling limit on $appName ($minutes mins). Step outside and touch real grass to unlock.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "🌱 Touch Grass",
                pendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(packageName.hashCode(), notification)
        } catch (_: SecurityException) {}
    }
}
