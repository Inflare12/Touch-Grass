package com.example.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.TouchGrassApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TouchGrassAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var lastInterventionTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return

        // Skip our own app
        if (packageName == applicationContext.packageName) return

        serviceScope.launch {
            val app = application as? TouchGrassApp ?: return@launch
            val limit = app.repository.getAppUsageLimit(packageName) ?: return@launch

            if (limit.isMonitored) {
                val now = System.currentTimeMillis()
                // Throttle interventions so user isn't looped infinitely
                if (now - lastInterventionTime > 20000) {
                    lastInterventionTime = now

                    val intent = Intent(applicationContext, InterventionActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(InterventionActivity.EXTRA_PACKAGE_NAME, packageName)
                        putExtra(InterventionActivity.EXTRA_APP_NAME, limit.appName)
                        putExtra(InterventionActivity.EXTRA_SCROLLING_MINUTES, limit.customLimitMinutes)
                    }
                    startActivity(intent)
                }
            }
        }
    }

    override fun onInterrupt() {}
}
