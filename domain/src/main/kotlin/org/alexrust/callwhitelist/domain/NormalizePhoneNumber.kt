package org.alexrust.callwhitelist.domain

class NormalizePhoneNumber {
    operator fun invoke(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        val normalized = buildString {
            trimmed.forEachIndexed { index, char ->
                if (char.isDigit() || (char == '+' && index == 0)) append(char)
            }
        }
        return normalized.takeIf { it.count(Char::isDigit) >= 5 }
    }
}
