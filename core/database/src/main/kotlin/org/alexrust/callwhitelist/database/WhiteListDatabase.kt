package org.alexrust.callwhitelist.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [NumberRuleEntity::class, CallLogEntity::class], version = 3, exportSchema = false)
abstract class WhiteListDatabase : RoomDatabase() {
    abstract fun numberRuleDao(): NumberRuleDao
    abstract fun callLogDao(): CallLogDao
}
