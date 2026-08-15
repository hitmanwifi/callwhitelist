package org.alexrust.callwhitelist.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NumberRuleDao {
    @Query("SELECT * FROM number_rules ORDER BY id DESC")
    fun observeAll(): Flow<List<NumberRuleEntity>>

    @Query("SELECT * FROM number_rules ORDER BY id DESC")
    suspend fun getAll(): List<NumberRuleEntity>

    @Insert
    suspend fun insert(rule: NumberRuleEntity): Long

    @Update
    suspend fun update(rule: NumberRuleEntity)

    @Delete
    suspend fun delete(rule: NumberRuleEntity)
}
