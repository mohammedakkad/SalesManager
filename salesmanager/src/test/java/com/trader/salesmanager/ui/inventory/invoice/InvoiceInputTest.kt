package com.trader.salesmanager.ui.inventory.invoice
import java.util.Locale
import org.junit.Test
import org.junit.Assert.*
class InvoiceInputTest {
    @Test fun latinAndPersianDigitsAreParsed() {
        assertEquals(12.5,"۱۲.۵".toSafeDouble()!!,0.0)
        assertEquals(12.5,"12,5".toSafeDouble()!!,0.0)
    }
    @Test fun arabicDecimalAndGroupingSeparatorsAreParsed() {
        assertEquals(1234.5,"١٬٢٣٤٫٥٠".toSafeDouble()!!,0.0)
        assertEquals(12.5,"١٢،٥".toSafeDouble()!!,0.0)
    }
    @Test fun nonFiniteAndMalformedFinancialInputsAreRejected() {
        for(s in listOf("NaN","Infinity","-Infinity","1e9999","abc","1.2.3")) assertNull(s,s.toSafeDouble())
    }
    @Test fun amountFormattingIsIndependentOfDeviceLocale() {
        val prior=Locale.getDefault()
        try { Locale.setDefault(Locale.forLanguageTag("ar"));assertEquals("1234.50",1234.5.formatAmount()) } finally { Locale.setDefault(prior) }
    }
    @Test fun quantityFormattingKeepsFractionalStock() {
        assertEquals("1.125",1.125.formatQty());assertEquals("2",2.0.formatQty());assertEquals("0",0.0.formatQty())
    }
    @Test fun supportedWeightConversionsRoundTrip() {
        for(unit in SaleWeightUnit.entries) assertEquals(2.5,fromKgQuantity(toKgQuantity(2.5,unit),unit),0.0000001)
    }
}
