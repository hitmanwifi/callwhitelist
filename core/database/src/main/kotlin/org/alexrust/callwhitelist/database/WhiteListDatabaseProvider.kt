package org.alexrust.callwhitelist.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object WhiteListDatabaseProvider {
    @Volatile
    private var instance: WhiteListDatabase? = null

    fun get(context: Context): WhiteListDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WhiteListDatabase::class.java,
                "call_whitelist.db",
            ).addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build().also { instance = it }
        }
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE number_rules ADD COLUMN expiresAtMillis INTEGER")
    }
}
