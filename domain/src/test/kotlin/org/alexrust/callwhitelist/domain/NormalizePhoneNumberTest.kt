package org.alexrust.callwhitelist.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class NormalizePhoneNumberTest {
    @Test fun removesFormatting() {
        assertEquals("+79991234567", NormalizePhoneNumber()("+7 (999) 123-45-67"))
    }
}
