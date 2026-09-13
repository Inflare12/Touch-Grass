package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.BadgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {
    @Query("SELECT * FROM badges ORDER BY unlocked DESC, id ASC")
    fun getAllBadges(): Flow<List<BadgeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialBadges(badges: List<BadgeEntity>)

    @Query("UPDATE badges SET unlocked = 1, unlockedAt = :unlockedAt WHERE id = :id")
    suspend fun unlockBadge(id: String, unlockedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM badges WHERE unlocked = 1")
    fun getUnlockedBadgesCount(): Flow<Int>

    @Query("UPDATE badges SET unlocked = 0, unlockedAt = NULL")
    suspend fun resetBadges()
}
