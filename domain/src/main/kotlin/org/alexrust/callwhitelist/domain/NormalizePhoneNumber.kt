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
        if (normalized.count(Char::isDigit) < 5) return null
        return canonicalRussianNumber(normalized)
    }

    private fun canonicalRussianNumber(value: String): String {
        val digits = value.filter(Char::isDigit)
        return if (digits.length == RUSSIAN_NUMBER_LENGTH && digits.startsWith(RUSSIAN_TRUNK_PREFIX)) {
            "+7${digits.drop(1)}"
        } else {
            value
        }
    }

    private companion object {
        const val RUSSIAN_NUMBER_LENGTH = 11
        const val RUSSIAN_TRUNK_PREFIX = "8"
    }
}

/** PhoneLookup inputs preserving both Russian national and international forms. */
class PhoneNumberVariants(private val normalize: NormalizePhoneNumber = NormalizePhoneNumber()) {
    operator fun invoke(value: String?): List<String> {
        val original = value?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        val canonical = normalize(original) ?: return listOf(original)
        val digits = canonical.filter(Char::isDigit)
        val russianNational = if (canonical.startsWith("+7") && digits.length == 11) {
            "8${digits.drop(1)}"
        } else {
            null
        }
        return listOfNotNull(original, canonical, russianNational).distinct()
    }
}
