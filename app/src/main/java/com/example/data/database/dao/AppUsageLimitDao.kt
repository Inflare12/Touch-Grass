package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entity.AppUsageLimit
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageLimitDao {
    @Query("SELECT * FROM app_usage_limits")
    fun getAllLimits(): Flow<List<AppUsageLimit>>

    @Query("SELECT * FROM app_usage_limits WHERE isMonitored = 1")
    fun getMonitoredLimits(): Flow<List<AppUsageLimit>>

    @Query("SELECT * FROM app_usage_limits WHERE packageName = :packageName LIMIT 1")
    suspend fun getLimitForPackage(packageName: String): AppUsageLimit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(limit: AppUsageLimit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(limits: List<AppUsageLimit>)

    @Query("UPDATE app_usage_limits SET isMonitored = :isMonitored WHERE packageName = :packageName")
    suspend fun updateMonitoredStatus(packageName: String, isMonitored: Boolean)

    @Query("UPDATE app_usage_limits SET customLimitMinutes = :limitMinutes WHERE packageName = :packageName")
    suspend fun updateLimitMinutes(packageName: String, limitMinutes: Int)

    @Query("DELETE FROM app_usage_limits WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
