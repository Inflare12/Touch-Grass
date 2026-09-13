package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.ChallengeRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenge_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<ChallengeRecord>>

    @Query("SELECT * FROM challenge_records WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getRecordsSince(sinceTimestamp: Long): Flow<List<ChallengeRecord>>

    @Query("SELECT COUNT(*) FROM challenge_records WHERE method = 'TOUCH_GRASS' AND verifiedSuccessfully = 1")
    fun getTotalGrassTouchesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM challenge_records WHERE method = 'REWARDED_AD_BYPASS'")
    fun getTotalBypassesCount(): Flow<Int>

    @Query("SELECT * FROM challenge_records ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRecord(): ChallengeRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ChallengeRecord): Long

    @Query("DELETE FROM challenge_records")
    suspend fun deleteAll()
}
