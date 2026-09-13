package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_limits")
data class AppUsageLimit(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isMonitored: Boolean = true,
    val customLimitMinutes: Int = 30 // default 30 mins
)
