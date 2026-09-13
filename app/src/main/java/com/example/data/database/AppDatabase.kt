package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.dao.AppUsageLimitDao
import com.example.data.database.dao.BadgeDao
import com.example.data.database.dao.ChallengeDao
import com.example.data.database.dao.StreakDao
import com.example.data.database.entity.AppUsageLimit
import com.example.data.database.entity.BadgeEntity
import com.example.data.database.entity.ChallengeRecord
import com.example.data.database.entity.UserStreak

@Database(
    entities = [
        ChallengeRecord::class,
        AppUsageLimit::class,
        UserStreak::class,
        BadgeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun challengeDao(): ChallengeDao
    abstract fun appUsageLimitDao(): AppUsageLimitDao
    abstract fun streakDao(): StreakDao
    abstract fun badgeDao(): BadgeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "touch_grass_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
