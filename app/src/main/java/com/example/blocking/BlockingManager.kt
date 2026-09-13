package com.example.blocking

import android.content.Intent

interface BlockingManager {
    fun launchIntervention(packageName: String, appName: String, scrollingMinutes: Int)
    fun isAccessibilityServiceEnabled(): Boolean
    fun getAccessibilitySettingsIntent(): Intent
    fun canDrawOverlays(): Boolean
    fun getOverlaySettingsIntent(): Intent
    fun startMonitoringService()
    fun stopMonitoringService()
}
