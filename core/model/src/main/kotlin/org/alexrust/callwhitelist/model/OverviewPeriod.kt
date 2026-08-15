package org.alexrust.callwhitelist.model

enum class OverviewPeriod(val storageValue: String) {
    TODAY("today"),
    LAST_7_DAYS("last_7_days"),
    LAST_30_DAYS("last_30_days"),
    ALL("all"),
    ;

    companion object {
        fun fromStorage(value: String): OverviewPeriod =
            entries.firstOrNull { it.storageValue == value } ?: TODAY
    }
}
