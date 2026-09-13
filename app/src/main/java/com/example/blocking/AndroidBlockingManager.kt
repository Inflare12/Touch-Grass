package com.example.blocking

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat

class AndroidBlockingManager(private val context: Context) : BlockingManager {

    override fun isAppLimitExceeded(packageName: String): Boolean {
        return false
    }

    override fun launchIntervention(packageName: String, appName: String, scrollingMinutes: Int) {
        val intent = Intent(context, InterventionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(InterventionActivity.EXTRA_APP_NAME, appName)
            putExtra(InterventionActivity.EXTRA_SCROLLING_MINUTES, scrollingMinutes)
        }
        context.startActivity(intent)
    }

    override fun isAccessibilityServiceEnabled(): Boolean {
        val expectedServiceName = "${context.packageName}/${TouchGrassAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    override fun getAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    override fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    override fun getOverlaySettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent()
        }
    }

    override fun startMonitoringService() {
        val intent = Intent(context, TouchGrassMonitoringService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stopMonitoringService() {
        val intent = Intent(context, TouchGrassMonitoringService::class.java)
        context.stopService(intent)
    }
}
