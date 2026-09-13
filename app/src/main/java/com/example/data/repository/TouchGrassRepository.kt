package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.database.entity.AppUsageLimit
import com.example.data.database.entity.BadgeEntity
import com.example.data.database.entity.ChallengeRecord
import com.example.data.database.entity.UserStreak
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TouchGrassRepository(private val db: AppDatabase) {

    private val challengeDao = db.challengeDao()
    private val appUsageLimitDao = db.appUsageLimitDao()
    private val streakDao = db.streakDao()
    private val badgeDao = db.badgeDao()

    val allRecords: Flow<List<ChallengeRecord>> = challengeDao.getAllRecords()
    val totalGrassTouchesCount: Flow<Int> = challengeDao.getTotalGrassTouchesCount()
    val totalBypassesCount: Flow<Int> = challengeDao.getTotalBypassesCount()
    val streakFlow: Flow<UserStreak?> = streakDao.getStreakFlow()
    val allLimits: Flow<List<AppUsageLimit>> = appUsageLimitDao.getAllLimits()
    val monitoredLimits: Flow<List<AppUsageLimit>> = appUsageLimitDao.getMonitoredLimits()
    val allBadges: Flow<List<BadgeEntity>> = badgeDao.getAllBadges()

    suspend fun initializeBadges() {
        val defaultBadges = listOf(
            BadgeEntity(
                id = "first_contact",
                title = "First Contact",
                description = "Successfully touched grass for the first time.",
                iconEmoji = "🌱"
            ),
            BadgeEntity(
                id = "streak_3",
                title = "3-Day Nature Habit",
                description = "Touched grass 3 consecutive days.",
                iconEmoji = "🌿"
            ),
            BadgeEntity(
                id = "streak_7",
                title = "Weekend Outdoorsman",
                description = "Maintained a 7-day grass touching streak.",
                iconEmoji = "🏕️"
            ),
            BadgeEntity(
                id = "streak_30",
                title = "Touch Grass Veteran",
                description = "A whole month of reconnecting with reality.",
                iconEmoji = "🏆"
            ),
            BadgeEntity(
                id = "certified_toucher",
                title = "Certified Grass Toucher",
                description = "Completed 10 total grass touch challenges.",
                iconEmoji = "👑"
            ),
            BadgeEntity(
                id = "anti_cheat_survivor",
                title = "Pure Organic Soul",
                description = "Verified touch with 0 cheating attempts.",
                iconEmoji = "✨"
            )
        )
        badgeDao.insertInitialBadges(defaultBadges)
    }

    suspend fun recordGrassTouch(
        durationSeconds: Int,
        targetAppPackage: String? = null,
        targetAppName: String? = null,
        cheatAttempts: Int = 0
    ): ChallengeRecord {
        val record = ChallengeRecord(
            durationSeconds = durationSeconds,
            method = "TOUCH_GRASS",
            targetAppPackage = targetAppPackage,
            targetAppName = targetAppName,
            wasCheatingDetected = cheatAttempts > 0,
            cheatAttempts = cheatAttempts,
            verifiedSuccessfully = true
        )
        challengeDao.insertRecord(record)

        // Update streak
        val todayEpochDay = LocalDate.now().toEpochDay()
        val currentStreakObj = streakDao.getStreak() ?: UserStreak(id = 1)

        val newStreakCount: Int
        val newTodayCount: Int

        if (currentStreakObj.lastResetDayEpochDay != todayEpochDay) {
            // New day
            newTodayCount = 1
            newStreakCount = when (todayEpochDay - currentStreakObj.lastTouchDateEpochDay) {
                1L -> currentStreakObj.currentStreak + 1
                0L -> currentStreakObj.currentStreak // same day
                else -> 1 // missed days or first time
            }
        } else {
            // Already active today
            newTodayCount = currentStreakObj.grassTouchedTodayCount + 1
            newStreakCount = if (currentStreakObj.currentStreak == 0) 1 else currentStreakObj.currentStreak
        }

        val totalCompleted = currentStreakObj.totalChallengesCompleted + 1
        val highest = maxOf(currentStreakObj.highestStreak, newStreakCount)

        streakDao.insertOrUpdate(
            currentStreakObj.copy(
                currentStreak = newStreakCount,
                highestStreak = highest,
                lastTouchDateEpochDay = todayEpochDay,
                totalChallengesCompleted = totalCompleted,
                grassTouchedTodayCount = newTodayCount,
                lastResetDayEpochDay = todayEpochDay
            )
        )

        // Unlock badges check
        badgeDao.unlockBadge("first_contact")
        if (cheatAttempts == 0) {
            badgeDao.unlockBadge("anti_cheat_survivor")
        }
        if (newStreakCount >= 3) badgeDao.unlockBadge("streak_3")
        if (newStreakCount >= 7) badgeDao.unlockBadge("streak_7")
        if (newStreakCount >= 30) badgeDao.unlockBadge("streak_30")
        if (totalCompleted >= 10) badgeDao.unlockBadge("certified_toucher")

        return record
    }

    suspend fun recordAdBypass(
        targetAppPackage: String? = null,
        targetAppName: String? = null
    ): ChallengeRecord {
        val record = ChallengeRecord(
            durationSeconds = 15,
            method = "REWARDED_AD_BYPASS",
            targetAppPackage = targetAppPackage,
            targetAppName = targetAppName,
            verifiedSuccessfully = true
        )
        challengeDao.insertRecord(record)

        val currentStreakObj = streakDao.getStreak() ?: UserStreak(id = 1)
        streakDao.insertOrUpdate(
            currentStreakObj.copy(
                totalBypassesUsed = currentStreakObj.totalBypassesUsed + 1
            )
        )
        return record
    }

    suspend fun getAppUsageLimit(packageName: String): AppUsageLimit? {
        return appUsageLimitDao.getLimitForPackage(packageName)
    }

    suspend fun updateAppMonitoring(packageName: String, appName: String, isMonitored: Boolean, limitMinutes: Int) {
        appUsageLimitDao.insertOrUpdate(
            AppUsageLimit(
                packageName = packageName,
                appName = appName,
                isMonitored = isMonitored,
                customLimitMinutes = limitMinutes
            )
        )
    }

    suspend fun resetAllData() {
        challengeDao.deleteAll()
        streakDao.reset()
        badgeDao.resetBadges()
        initializeBadges()
    }
}
