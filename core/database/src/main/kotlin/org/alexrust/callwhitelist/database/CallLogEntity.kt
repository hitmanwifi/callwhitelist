package org.alexrust.callwhitelist.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_logs",
    indices = [Index(value = ["timestampMillis"]), Index(value = ["eventId"], unique = true)],
)
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: String,
    val timestampMillis: Long,
    val number: String?,
    val decision: String,
    val source: String,
    val reason: String,
)
