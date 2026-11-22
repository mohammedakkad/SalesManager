package com.trader.core.util.format

import androidx.compose.runtime.mutableStateOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The single place that decides how the app prints numbers, money, quantities and dates.
 *
 * Arithmetic and rounding are unchanged from the historical helpers: values are first
 * formatted with [Locale.US] (HALF_UP, '.' decimal separator) and only then mapped to the
 * selected [digits]. Switching [digits] to [DigitSystem.LATIN] changes every output at once.
 *
 * Every function here returns a *bidi-isolated* run (LRI … PDI) so that signs, separators,
 * currency symbols and identifiers keep their order inside right-to-left text instead of
 * being reordered by the paragraph direction.
 */
object AppNumberFormat {

    enum class DigitSystem { ARABIC_INDIC, LATIN }

    /** Default for new installs; the user can switch in Settings ("نظام الأرقام"). */
    val DEFAULT_DIGITS = DigitSystem.ARABIC_INDIC

    // Snapshot state: composables that format numbers recompose when the user changes the setting.
    private val digitState = mutableStateOf(DEFAULT_DIGITS)

    /** The active digit system for every UI and PDF output (persisted by DigitSystemPreference). */
    var digits: DigitSystem
        get() = digitState.value
        set(value) { digitState.value = value }

    /**
     * Locale for display text that contains words (month/day names): Arabic names with Latin
     * digits, which are then mapped by [localizeDigits]. Never the device locale, so an English
     * device shows the same output as an Arabic one.
     */
    val DISPLAY_LOCALE: Locale = Locale.forLanguageTag("ar-u-nu-latn")

    const val CURRENCY = "₪"

    /** Left-to-right isolate / pop directional isolate (Unicode 6.3, supported by Android's ICU bidi). */
    const val LRI = '⁦'
    const val PDI = '⁩'

    private const val ARABIC_ZERO = '٠'
    private const val ARABIC_DECIMAL = '٫'
    private const val ARABIC_GROUPING = '٬'

    /** Maps ASCII digits and US separators to the selected digit system; other characters are kept. */
    fun localizeDigits(text: String): String {
        if (digits == DigitSystem.LATIN) return text
        val out = StringBuilder(text.length)
        for (ch in text) {
            out.append(
                when (ch) {
                    in '0'..'9' -> ARABIC_ZERO + (ch - '0')
                    else -> ch
                }
            )
        }
        return out.toString()
    }

    /** Wraps [text] in a left-to-right isolate. */
    fun isolate(text: String): String = "$LRI$text$PDI"

    /** 1234.5 -> ١٬٢٣٤٫٥٠ (two decimals, grouped, HALF_UP like the previous formatAmount). */
    fun amount(value: Double, decimals: Int = 2): String = isolate(plainAmount(value, decimals))

    /** Signed money with the currency symbol, e.g. "₪ ١٠٠٫٠٠" or "-₪ ٢٥٫٠٠". */
    fun money(value: Double, decimals: Int = 2): String {
        val sign = if (value < 0.0 && plainAmount(-value, decimals) != plainAmount(0.0, decimals)) "-" else ""
        return isolate("$sign$CURRENCY ${plainAmount(kotlin.math.abs(value), decimals)}")
    }

    /** A deduction that is always printed with a leading minus, e.g. "-₪ ٢٥٫٠٠". */
    fun deduction(value: Double, decimals: Int = 2): String =
        isolate("-$CURRENCY ${plainAmount(kotlin.math.abs(value), decimals)}")

    /** Quantity with up to three decimals and trailing zeros removed (same as formatQty). */
    fun quantity(value: Double): String {
        val s = String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
        return isolate(localizeSeparators(if (s.isEmpty() || s == "-0") "0" else s))
    }

    fun integer(value: Long): String = isolate(localizeDigits(value.toString()))

    fun integer(value: Int): String = integer(value.toLong())

    /**
     * Document identifiers, e.g. "#٠٠٠٠٠٨". A single script per token: mixing a Latin prefix with
     * Arabic-Indic digits ("INV-٠٠٠٠٠٨") is reordered by PDF text extraction/search, so the
     * document type is carried by the surrounding label instead.
     */
    fun documentNumber(id: Long, width: Int = 6): String =
        isolate("#" + localizeDigits(id.toString().padStart(width, '0')))

    /** Reference to a record, e.g. "#٢١٠٠١". */
    fun reference(id: Long): String = isolate("#" + localizeDigits(id.toString()))

    fun date(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String =
        isolate(localizeDigits(pattern("dd/MM/yyyy", timeZone).format(Date(millis))))

    fun dateTime(millis: Long, timeZone: TimeZone = TimeZone.getDefault()): String =
        isolate(localizeDigits(pattern("dd/MM/yyyy HH:mm", timeZone).format(Date(millis))))

    /** "صفحة ٢ من ٥" */
    fun pageOf(page: Int, total: Int): String = "صفحة ${integer(page)} من ${integer(total)}"

    private fun plainAmount(value: Double, decimals: Int): String =
        localizeSeparators(String.format(Locale.US, "%,.${decimals}f", value))

    private fun localizeSeparators(usFormatted: String): String {
        if (digits == DigitSystem.LATIN) return usFormatted
        val out = StringBuilder(usFormatted.length)
        for (ch in usFormatted) {
            out.append(
                when (ch) {
                    in '0'..'9' -> ARABIC_ZERO + (ch - '0')
                    '.' -> ARABIC_DECIMAL
                    ',' -> ARABIC_GROUPING
                    else -> ch
                }
            )
        }
        return out.toString()
    }

    private fun pattern(pattern: String, timeZone: TimeZone) =
        SimpleDateFormat(pattern, Locale.US).apply { this.timeZone = timeZone }

    // ── Display helpers (UI): same output as the historical call sites, digits localized ──────────
    // They do not add bidi isolates, so plain UI strings stay byte-identical in Western mode.

    /** e.g. number(12.5, "%,.2f") -> "١٢٫٥٠" — any java.util.Formatter pattern with one numeric argument. */
    fun number(value: Number, pattern: String = "%.2f"): String =
        localizeSeparators(String.format(Locale.US, pattern, value))

    /** Quantity with up to three decimals, trailing zeros removed (same rule as [quantity]). */
    fun plainQuantity(value: Double): String {
        val s = String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
        return localizeSeparators(if (s.isEmpty() || s == "-0") "0" else s)
    }

    /** Date/time text for display with Arabic month/day names, e.g. formatDate("dd/MM hh:mm a", millis). */
    fun formatDate(pattern: String, millis: Long): String =
        localizeDigits(dateFormat(pattern).format(Date(millis)))

    fun formatDate(pattern: String, date: Date): String = formatDate(pattern, date.time)

    private val dateFormats = object : ThreadLocal<HashMap<String, SimpleDateFormat>>() {
        override fun initialValue() = HashMap<String, SimpleDateFormat>()
    }

    /** One cached SimpleDateFormat per pattern and thread (SimpleDateFormat is not thread-safe). */
    private fun dateFormat(pattern: String): SimpleDateFormat {
        val cache = dateFormats.get()!!
        return cache.getOrPut(pattern) { SimpleDateFormat(pattern, DISPLAY_LOCALE) }.also { it.timeZone = TimeZone.getDefault() }
    }

    // ── Input parsing: accepts both digit systems in every field ─────────────────────────────

    /**
     * Parses user input typed with Western, Arabic-Indic (٠-٩) or Persian (۰-۹) digits.
     * '٫', '،' and ',' are decimal separators, '٬' is a grouping separator; leading/trailing
     * spaces and bidi marks are ignored. Returns null for empty, malformed or non-finite input.
     */
    fun parseNumber(input: String): Double? = normalizeDigits(input).toDoubleOrNull()?.takeIf { it.isFinite() }

    /** The normalization used by [parseNumber]: digits to ASCII, separators to '.', marks removed. */
    fun normalizeDigits(input: String): String {
        val out = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '\u0660'..'\u0669' -> out.append('0' + (ch - '\u0660'))
                in '\u06F0'..'\u06F9' -> out.append('0' + (ch - '\u06F0'))
                '\u066C' -> Unit // Arabic thousands separator
                '\u066B', '\u060C', ',' -> out.append('.')
                '\u2066', '\u2067', '\u2068', '\u2069', '\u200E', '\u200F', '\u061C' -> Unit
                '\u2212' -> out.append('-') // minus sign
                else -> out.append(ch)
            }
        }
        return out.toString().trim()
    }
}
