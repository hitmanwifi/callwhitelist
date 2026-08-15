package org.alexrust.callwhitelist.database

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import kotlinx.coroutines.flow.Flow
import androidx.room.Delete
import androidx.room.Update
import androidx.room.Index

@Entity(tableName = "number_rules")
data class NumberRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val label: String,
    val enabled: Boolean,
    val decision: String,
)

@Entity(
    tableName = "call_logs",
    indices = [Index(value = ["timestampMillis"])]
)
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val number: String?,
    val decision: String,
    val source: String,
    val reason: String,
)

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

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<CallLogEntity>>

    @Query("SELECT COUNT(*) FROM call_logs WHERE decision = 'BLOCK' AND timestampMillis > :sinceMillis")
    fun observeBlockedCountSince(sinceMillis: Long): Flow<Int>

    @Insert
    suspend fun insert(entry: CallLogEntity)

    @Query("DELETE FROM call_logs")
    suspend fun clear()
}

@Database(entities = [NumberRuleEntity::class, CallLogEntity::class], version = 2, exportSchema = false)
abstract class WhiteListDatabase : RoomDatabase() {
    abstract fun numberRuleDao(): NumberRuleDao
    abstract fun callLogDao(): CallLogDao
}

object WhiteListDatabaseProvider {
    @Volatile
    private var instance: WhiteListDatabase? = null

    fun get(context: android.content.Context): WhiteListDatabase {
        return instance ?: synchronized(this) {
            instance ?: androidx.room.Room.databaseBuilder(
                context.applicationContext,
                WhiteListDatabase::class.java,
                "call_whitelist.db",
            ).fallbackToDestructiveMigration(dropAllTables = true).build().also { instance = it }
        }
    }
}
