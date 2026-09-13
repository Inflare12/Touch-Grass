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
        badgeDao.insertInitialBadges(
            listOf(
                BadgeEntity("first_contact", "First Contact", "Successfully touched grass for the first time.", "🌱"),
                BadgeEntity("streak_3", "3-Day Nature Habit", "Touched grass 3 consecutive days.", "🌿"),
                BadgeEntity("streak_7", "Weekend Outdoorsman", "Maintained a 7-day grass touching streak.", "🏕️"),
                BadgeEntity("streak_30", "Touch Grass Veteran", "A whole month of reconnecting with reality.", "🏆"),
                BadgeEntity("certified_toucher", "Certified Grass Toucher", "Completed 10 total grass touch challenges.", "👑"),
                BadgeEntity("anti_cheat_survivor", "Pure Organic Soul", "Verified touch with 0 cheating attempts.", "✨")
            )
        )
    }

    suspend fun recordGrassTouch(durationSeconds: Int, targetAppPackage: String? = null, targetAppName: String? = null, cheatAttempts: Int = 0): ChallengeRecord {
        val record = ChallengeRecord(
            durationSeconds = durationSeconds.coerceAtLeast(0),
            method = "TOUCH_GRASS",
            targetAppPackage = targetAppPackage,
            targetAppName = targetAppName,
            wasCheatingDetected = cheatAttempts > 0,
            cheatAttempts = cheatAttempts.coerceAtLeast(0),
            verifiedSuccessfully = true
        )
        challengeDao.insertRecord(record)

        val todayEpochDay = LocalDate.now().toEpochDay()
        val current = streakDao.getStreak() ?: UserStreak(id = 1)
        val newTodayCount: Int
        val newStreakCount: Int

        if (current.lastResetDayEpochDay != todayEpochDay) {
            newTodayCount = 1
            newStreakCount = when (todayEpochDay - current.lastTouchDateEpochDay) {
                1L -> current.currentStreak + 1
                0L -> current.currentStreak.coerceAtLeast(1)
                else -> 1
            }
        } else {
            newTodayCount = current.grassTouchedTodayCount + 1
            newStreakCount = current.currentStreak.coerceAtLeast(1)
        }

        val totalCompleted = current.totalChallengesCompleted + 1
        streakDao.insertOrUpdate(
            current.copy(
                currentStreak = newStreakCount,
                highestStreak = maxOf(current.highestStreak, newStreakCount),
                lastTouchDateEpochDay = todayEpochDay,
                totalChallengesCompleted = totalCompleted,
                grassTouchedTodayCount = newTodayCount,
                lastResetDayEpochDay = todayEpochDay
            )
        )

        badgeDao.unlockBadge("first_contact")
        if (cheatAttempts == 0) badgeDao.unlockBadge("anti_cheat_survivor")
        if (newStreakCount >= 3) badgeDao.unlockBadge("streak_3")
        if (newStreakCount >= 7) badgeDao.unlockBadge("streak_7")
        if (newStreakCount >= 30) badgeDao.unlockBadge("streak_30")
        if (totalCompleted >= 10) badgeDao.unlockBadge("certified_toucher")
        return record
    }

    suspend fun recordAdBypass(targetAppPackage: String? = null, targetAppName: String? = null): ChallengeRecord {
        val record = ChallengeRecord(
            // An ad bypass is not a grass challenge, so it must not pretend to have a 15s challenge duration.
            durationSeconds = 0,
            method = "REWARDED_AD_BYPASS",
            targetAppPackage = targetAppPackage,
            targetAppName = targetAppName,
            verifiedSuccessfully = true
        )
        challengeDao.insertRecord(record)
        val current = streakDao.getStreak() ?: UserStreak(id = 1)
        streakDao.insertOrUpdate(current.copy(totalBypassesUsed = current.totalBypassesUsed + 1))
        return record
    }

    suspend fun getAppUsageLimit(packageName: String): AppUsageLimit? = appUsageLimitDao.getLimitForPackage(packageName)

    suspend fun updateAppMonitoring(packageName: String, appName: String, isMonitored: Boolean, limitMinutes: Int) {
        appUsageLimitDao.insertOrUpdate(
            AppUsageLimit(
                packageName = packageName,
                appName = appName,
                isMonitored = isMonitored,
                customLimitMinutes = limitMinutes.coerceIn(1, 24 * 60)
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
