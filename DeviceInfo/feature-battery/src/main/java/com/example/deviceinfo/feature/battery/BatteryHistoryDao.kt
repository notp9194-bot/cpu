package com.example.deviceinfo.feature.battery

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BatteryHistoryEntity)

    /** Last 24 hours */
    @Query("SELECT * FROM battery_history WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun getLast24Hours(since: Long): Flow<List<BatteryHistoryEntity>>

    /** Last N rows for quick chart */
    @Query("SELECT * FROM battery_history ORDER BY timestamp DESC LIMIT :n")
    suspend fun getLastN(n: Int): List<BatteryHistoryEntity>

    /** Delete entries older than 48 hours to keep DB small */
    @Query("DELETE FROM battery_history WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
