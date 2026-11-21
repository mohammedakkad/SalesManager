package com.trader.salesmanager.util.format

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class AppNumberFormatTest {
    private fun bare(s: String) = s.removePrefix(AppNumberFormat.LRI.toString()).removeSuffix(AppNumberFormat.PDI.toString())

    @After fun reset() { AppNumberFormat.digits = AppNumberFormat.DigitSystem.ARABIC_INDIC }

    @Test fun amountsUseArabicIndicDigitsSeparatorsAndHalfUpRounding() {
        assertEquals("١٠٠٫٠٠", bare(AppNumberFormat.amount(100.0)))
        assertEquals("١٢٬٣٤٥٫٦٧", bare(AppNumberFormat.amount(12345.67)))
        // Same rounding as the previous Locale.US formatAmount: 39.375 -> 39.38, 5.625 -> 5.63.
        assertEquals("٣٩٫٣٨", bare(AppNumberFormat.amount(39.375)))
        assertEquals("٥٫٦٣", bare(AppNumberFormat.amount(5.625)))
    }

    @Test fun moneyKeepsSignAndCurrencyInOneIsolatedRun() {
        val m = AppNumberFormat.money(-25.0)
        assertTrue(m.first() == AppNumberFormat.LRI && m.last() == AppNumberFormat.PDI)
        assertEquals("-₪ ٢٥٫٠٠", bare(m))
        assertEquals("₪ ٠٫٠٠", bare(AppNumberFormat.money(-0.001)))
        assertEquals("-₪ ٨٠٫٠٠", bare(AppNumberFormat.deduction(80.0)))
    }

    @Test fun quantitiesIdentifiersAndDates() {
        assertEquals("٢٫٢٥", bare(AppNumberFormat.quantity(2.25)))
        assertEquals("٤", bare(AppNumberFormat.quantity(4.0)))
        assertEquals("٠", bare(AppNumberFormat.quantity(0.0)))
        assertEquals("INV-٠٠٠٣٠١", bare(AppNumberFormat.documentNumber("INV-", 301)))
        assertEquals("#٢١٠٠١", bare(AppNumberFormat.reference(21001)))
        assertEquals("٠١/٠١/٢٠٢٦", bare(AppNumberFormat.date(1767225600000, TimeZone.getTimeZone("UTC"))))
        assertEquals("صفحة ${AppNumberFormat.integer(2)} من ${AppNumberFormat.integer(5)}", AppNumberFormat.pageOf(2, 5))
    }

    @Test fun singleSwitchRestoresLatinDigitsEverywhere() {
        AppNumberFormat.digits = AppNumberFormat.DigitSystem.LATIN
        assertEquals("12,345.67", bare(AppNumberFormat.amount(12345.67)))
        assertEquals("INV-000008", bare(AppNumberFormat.documentNumber("INV-", 8)))
        assertEquals("abc 123", AppNumberFormat.localizeDigits("abc 123"))
    }
}
