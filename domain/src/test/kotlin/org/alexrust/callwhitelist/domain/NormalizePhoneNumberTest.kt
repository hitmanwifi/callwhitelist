package org.alexrust.callwhitelist.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class NormalizePhoneNumberTest {
    @Test fun removesFormatting() {
        assertEquals("+79991234567", NormalizePhoneNumber()("+7 (999) 123-45-67"))
    }

    @Test fun canonicalizesRussianTrunkPrefixToCountryCode() {
        assertEquals("+79120000001", NormalizePhoneNumber()("89120000001"))
    }

    @Test fun exposesBothRussianLookupForms() {
        assertEquals(
            listOf("89120000001", "+79120000001"),
            PhoneNumberVariants()("89120000001"),
        )
    }
}
