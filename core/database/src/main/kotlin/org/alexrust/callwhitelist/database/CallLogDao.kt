package org.alexrust.callwhitelist.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<CallLogEntity>>

    @Query("SELECT COUNT(*) FROM call_logs WHERE decision = 'BLOCK' AND timestampMillis > :sinceMillis")
    fun observeBlockedCountSince(sinceMillis: Long): Flow<Int>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM call_logs " +
            "WHERE timestampMillis = :timestampMillis AND number IS :number)",
    )
    suspend fun containsEvent(timestampMillis: Long, number: String?): Boolean

    @Insert
    suspend fun insert(entry: CallLogEntity)

    @Transaction
    suspend fun insertIfAbsent(entry: CallLogEntity) {
        if (!containsEvent(entry.timestampMillis, entry.number)) {
            insert(entry)
        }
    }

    @Query("DELETE FROM call_logs")
    suspend fun clear()
}
