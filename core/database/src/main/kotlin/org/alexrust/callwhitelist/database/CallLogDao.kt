package org.alexrust.callwhitelist.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<CallLogEntity>>

    @Query("SELECT COUNT(*) FROM call_logs WHERE decision = 'BLOCK' AND timestampMillis > :sinceMillis")
    fun observeBlockedCountSince(sinceMillis: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entry: CallLogEntity): Long

    @Query("DELETE FROM call_logs")
    suspend fun clear()
}
