package org.alexrust.callwhitelist.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "number_rules")
data class NumberRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val label: String,
    val enabled: Boolean,
    val decision: String,
    val expiresAtMillis: Long?,
)
