package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenge_records")
data class ChallengeRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int,
    val method: String, // "TOUCH_GRASS" or "REWARDED_AD_BYPASS"
    val targetAppPackage: String? = null,
    val targetAppName: String? = null,
    val wasCheatingDetected: Boolean = false,
    val cheatAttempts: Int = 0,
    val verifiedSuccessfully: Boolean = true
)
