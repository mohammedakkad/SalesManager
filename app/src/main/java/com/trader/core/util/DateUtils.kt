package com.trader.core.util

import com.trader.core.util.format.AppNumberFormat
import java.util.Calendar

object DateUtils {
    // Display formatting goes through the central formatter (device-independent, thread-safe).
    fun Long.toDateString(): String = AppNumberFormat.formatDate("dd/MM/yyyy", this)
    fun Long.toTimeString(): String = AppNumberFormat.formatDate("hh:mm a", this)
    fun Long.toDateTimeString(): String = toDateString() + " " + toTimeString()

    fun todayStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun todayEnd(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
