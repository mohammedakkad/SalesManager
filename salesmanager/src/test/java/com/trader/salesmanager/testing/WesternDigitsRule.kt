package com.trader.salesmanager.testing

import com.trader.core.util.format.AppNumberFormat
import org.junit.rules.ExternalResource

/**
 * Pins Western digits for tests whose fixtures assert numeric text written with 0-9.
 * The app default is Arabic-Indic (see AppNumberFormat.DEFAULT_DIGITS); that default is covered
 * by dedicated tests (DigitSystemUiTest, AppNumberFormatTest), so these tests keep verifying
 * their own behavior independent of the digit style.
 */
class WesternDigitsRule : ExternalResource() {
    private var previous = AppNumberFormat.DEFAULT_DIGITS
    override fun before() {
        previous = AppNumberFormat.digits
        AppNumberFormat.digits = AppNumberFormat.DigitSystem.LATIN
    }
    override fun after() {
        AppNumberFormat.digits = previous
    }
}
