package com.trader.core.util.format

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals("#٠٠٠٣٠١", bare(AppNumberFormat.documentNumber(301)))
        assertEquals("#٢١٠٠١", bare(AppNumberFormat.reference(21001)))
        assertEquals("٠١/٠١/٢٠٢٦", bare(AppNumberFormat.date(1767225600000, TimeZone.getTimeZone("UTC"))))
        assertEquals("صفحة ${AppNumberFormat.integer(2)} من ${AppNumberFormat.integer(5)}", AppNumberFormat.pageOf(2, 5))
    }

    @Test fun singleSwitchRestoresLatinDigitsEverywhere() {
        AppNumberFormat.digits = AppNumberFormat.DigitSystem.LATIN
        assertEquals("12,345.67", bare(AppNumberFormat.amount(12345.67)))
        assertEquals("#000008", bare(AppNumberFormat.documentNumber(8)))
        assertEquals("abc 123", AppNumberFormat.localizeDigits("abc 123"))
    }

    @Test fun inputParsingAcceptsBothDigitSystemsAndArabicSeparators() {
        assertEquals(123.0, AppNumberFormat.parseNumber("١2٣")!!, 0.0)              // mixed digits
        assertEquals(12.5, AppNumberFormat.parseNumber("١٢٫٥")!!, 0.0)              // Arabic decimal separator
        assertEquals(1234.5, AppNumberFormat.parseNumber("١٬٢٣٤٫٥")!!, 0.0)         // Arabic grouping separator
        assertEquals(12.5, AppNumberFormat.parseNumber("12,5")!!, 0.0)              // comma typed as decimal
        assertEquals(12.0, AppNumberFormat.parseNumber("۱۲")!!, 0.0)                // Persian digits
        assertEquals(-5.0, AppNumberFormat.parseNumber("-٥")!!, 0.0)                // negative
        assertEquals(-5.0, AppNumberFormat.parseNumber("\u2212٥")!!, 0.0)           // Unicode minus sign
        assertEquals(7.0, AppNumberFormat.parseNumber(" \u2066٧\u2069 ")!!, 0.0)    // pasted isolated text
        assertEquals(999_999_999_999.99, AppNumberFormat.parseNumber("٩٩٩٩٩٩٩٩٩٩٩٩٫٩٩")!!, 0.0)
        assertNull(AppNumberFormat.parseNumber(""))
        assertNull(AppNumberFormat.parseNumber("   "))
        assertNull(AppNumberFormat.parseNumber("abc"))
        assertNull(AppNumberFormat.parseNumber("1e400"))                            // not finite
        assertNull(AppNumberFormat.parseNumber("١٫٢٫٣"))                            // two separators
    }

    @Test fun displayHelpersKeepThePatternAndOnlyChangeDigits() {
        assertEquals("١٬٢٣٤٫٥٠", AppNumberFormat.number(1234.5, "%,.2f"))
        assertEquals("٣٩٫٣٨", AppNumberFormat.number(39.375))
        assertEquals("١٫٥ م", AppNumberFormat.number(1.5, "%.1f م"))
        assertEquals("٢٫٢٥", AppNumberFormat.plainQuantity(2.25))
        assertEquals("٠١/٠١", AppNumberFormat.formatDate("dd/MM", java.util.Calendar.getInstance().apply { clear(); set(2026, 0, 1) }.timeInMillis))
        AppNumberFormat.digits = AppNumberFormat.DigitSystem.LATIN
        assertEquals("1,234.50", AppNumberFormat.number(1234.5, "%,.2f"))
        assertEquals("01/01", AppNumberFormat.formatDate("dd/MM", java.util.Calendar.getInstance().apply { clear(); set(2026, 0, 1) }.timeInMillis))
    }

    @Test fun displayIgnoresTheDeviceLocale() {
        val device = java.util.Locale.getDefault()
        try {
            val millis = java.util.Calendar.getInstance().apply { clear(); set(2026, 0, 5) }.timeInMillis
            java.util.Locale.setDefault(java.util.Locale.ENGLISH)
            val english = AppNumberFormat.number(1234.5, "%,.2f") to AppNumberFormat.formatDate("d MMMM yyyy", millis)
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("ar"))
            val arabic = AppNumberFormat.number(1234.5, "%,.2f") to AppNumberFormat.formatDate("d MMMM yyyy", millis)
            assertEquals(arabic, english)
            assertTrue("Arabic month name expected: ${english.second}", english.second.contains("يناير"))
        } finally { java.util.Locale.setDefault(device) }
    }
}
