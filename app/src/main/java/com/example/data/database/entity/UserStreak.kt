package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_streak")
data class UserStreak(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int = 0,
    val highestStreak: Int = 0,
    val lastTouchDateEpochDay: Long = 0,
    val totalChallengesCompleted: Int = 0,
    val totalBypassesUsed: Int = 0,
    val grassTouchedTodayCount: Int = 0,
    val lastResetDayEpochDay: Long = 0
)
