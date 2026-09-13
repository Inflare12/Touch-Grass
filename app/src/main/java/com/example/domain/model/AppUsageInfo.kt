package com.example.domain.model

import android.graphics.drawable.Drawable

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val dailyUsageMillis: Long = 0L,
    val dailyLimitMinutes: Int = 30,
    val isMonitored: Boolean = false,
    val icon: Drawable? = null
) {
    val dailyUsageMinutes: Int
        get() = (dailyUsageMillis / 60000L).toInt()

    val isLimitExceeded: Boolean
        get() = isMonitored && dailyUsageMinutes >= dailyLimitMinutes

    val progressRatio: Float
        get() = if (dailyLimitMinutes <= 0) 0f else (dailyUsageMinutes.toFloat() / dailyLimitMinutes).coerceIn(0f, 1f)

    val formattedUsage: String
        get() {
            val hours = dailyUsageMinutes / 60
            val minutes = dailyUsageMinutes % 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
}
