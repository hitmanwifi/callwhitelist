package org.alexrust.callwhitelist.model

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun fromStorage(value: String): ThemeMode = entries.firstOrNull {
            it.storageValue == value
        } ?: SYSTEM
    }
}
