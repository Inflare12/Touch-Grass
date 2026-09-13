package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.UserStreak
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Query("SELECT * FROM user_streak WHERE id = 1 LIMIT 1")
    fun getStreakFlow(): Flow<UserStreak?>

    @Query("SELECT * FROM user_streak WHERE id = 1 LIMIT 1")
    suspend fun getStreak(): UserStreak?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(streak: UserStreak)

    @Query("DELETE FROM user_streak")
    suspend fun reset()
}
