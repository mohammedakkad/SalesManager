package com.trader.salesmanager.util.pdf

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.DailySalesProfit
import com.trader.core.domain.model.DebtAging
import com.trader.core.domain.model.InvoiceItem
import com.trader.core.domain.model.PaymentType
import com.trader.core.domain.model.ReturnSummary
import com.trader.core.domain.model.Transaction
import com.trader.core.domain.model.TransactionReturnStatus
import com.trader.salesmanager.util.pdf.components.PdfHeader
import com.trader.salesmanager.util.pdf.core.PdfDocumentBuilder
import com.trader.salesmanager.util.pdf.core.PdfFonts
import com.trader.salesmanager.util.pdf.design.PdfOptions
import com.trader.salesmanager.util.pdf.design.PdfSpacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Calendar
import java.util.TimeZone

/**
 * Renders the real generators through native Robolectric graphics into bitmaps (PNG previews in
 * build/pdf-previews) and checks pagination using the builder's layout log. This complements, and
 * does not replace, the device PdfDocument + PDFBox verification.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfLayoutRenderTest {
    private val out = File("build/pdf-previews")
    private val printedAt = 1767225600000L

    @Before fun fonts() {
        PdfFonts.install(ApplicationProvider.getApplicationContext())
        assertTrue("Bundled Arabic font must load", PdfFonts.isInstalled)
    }

    private fun sink(name: String) = BitmapPageSink(out, name)

    /** Every block is inside the content area; table blocks never overlap; tables repeat their header. */
    private fun assertWellFormed(builder: PdfDocumentBuilder, sink: BitmapPageSink) {
        assertEquals(builder.pageCount, sink.pages.size)
        val byPage = builder.layoutLog.groupBy { it.page }
        byPage.forEach { (page, entries) ->
            val top = PdfHeader.contentTop(continuation = page > 1)
            entries.forEach {
                assertTrue("Block ${it.kind} on page $page starts above content: ${it.top} < $top", it.top >= top - 0.01f)
                assertTrue("Block ${it.kind} on page $page is clipped: ${it.bottom} > ${PdfSpacing.CONTENT_BOTTOM}",
                    it.bottom <= PdfSpacing.CONTENT_BOTTOM + 0.01f)
            }
            val table = entries.filter { it.kind.startsWith("table") }.sortedBy { it.top }
            table.zipWithNext().forEach { (a, b) ->
                assertTrue("Overlapping ${a.kind}/${b.kind} on page $page", b.top >= a.bottom - 0.01f)
            }
            if (page > 1 && table.isNotEmpty() && table.first().kind != "table-header") {
                throw AssertionError("Page $page continues a table without repeating its header")
            }
        }
    }

    private fun item(i: Int, name: String = "ITEM-${i.toString().padStart(3, '0')}") =
        InvoiceItem(id = "item$i", productName = name, unitLabel = "قطعة", quantity = 1.0, pricePerUnit = 1.0, totalPrice = 1.0)

    @Test fun shortInvoiceFitsOnOnePage() {
        val sink = sink("invoice-short")
        val builder = InvoicePdfGenerator.render(
            InvoicePdfGenerator.InvoicePdfData(
                Transaction(id = 8, customerId = 7, amount = 100.0, isPaid = true, date = printedAt, paymentType = PaymentType.CASH,
                    paymentMethodName = "نقد", note = "يرجى الاحتفاظ بالفاتورة"),
                listOf(InvoiceItem(id = "i", productName = "أرز بسمتي ٥ كغ", unitLabel = "كيس", quantity = 4.0, pricePerUnit = 25.0, totalPrice = 100.0)),
                Customer(id = 7, name = "أحمد علي", phone = "0599123456"), "متجر الاختبار", printedAt = printedAt
            ), sink)
        assertEquals(1, builder.pageCount)
        assertWellFormed(builder, sink)
    }

    @Test fun longInvoicePaginatesEveryItemWithRepeatedHeaders() {
        val sink = sink("invoice-long")
        val builder = InvoicePdfGenerator.render(
            InvoicePdfGenerator.InvoicePdfData(
                Transaction(id = 9, customerId = 7, amount = 100.0, isPaid = false, date = printedAt),
                (0..99).map { item(it) }, null, "متجر طويل", printedAt = printedAt
            ), sink)
        assertTrue(builder.pageCount > 1)
        assertEquals(100, builder.layoutLog.count { it.kind == "table-row" })
        assertEquals(builder.layoutLog.map { it.page }.filter { p -> builder.layoutLog.any { it.page == p && it.kind == "table-row" } }.distinct().size,
            builder.layoutLog.count { it.kind == "table-header" })
        assertWellFormed(builder, sink)
    }

    @Test fun debtInvoiceWithReturnsAndLogo() {
        val sink = sink("invoice-debt-returns")
        val logo = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(16, 185, 129)) }
        val builder = InvoicePdfGenerator.render(
            InvoicePdfGenerator.InvoicePdfData(
                Transaction(id = 303, customerId = 7, customerName = "سامر", amount = 33.75, originalAmount = 39.375, isPaid = false,
                    paymentType = PaymentType.DEBT, date = printedAt, dueDate = printedAt + 7 * 86_400_000L, hasItems = true,
                    returnStatus = TransactionReturnStatus.PARTIALLY_RETURNED),
                listOf(
                    InvoiceItem("w", 303, "p", "جبنة بيضاء", "weight-unit", "كغ", 2.625, 15.0, 39.375),
                    InvoiceItem("x", 303, "q", "Pepsi 330ml", "can", "علبة", 6.0, 2.5, 15.0)
                ),
                Customer(7, "سامر"), "بقالة النور", logo = logo, priorDebtBalance = 10.0, currentDebtBalance = 43.75,
                returnSummary = ReturnSummary(TransactionReturnStatus.PARTIALLY_RETURNED, 5.625, mapOf("weight-unit" to .375)),
                printedAt = printedAt
            ), sink)
        assertWellFormed(builder, sink)
        logo.recycle()
    }

    @Test fun longCustomerStatementKeepsAllSixtyRows() {
        val sink = sink("statement-long")
        val rows = (1..60).map { Transaction(id = 21000L + it, customerId = 700, amount = it * 1.25, isPaid = it % 2 == 0,
            date = printedAt + it * 86_400_000L, paymentMethodName = if (it % 2 == 0) "CASH" else "") }
        val builder = CustomerStatementPdfGenerator.render(
            CustomerStatementPdfGenerator.StatementPdfData(Customer(700, "LONG-STATEMENT-CUSTOMER", phone = "0500000000"), rows,
                "LONG-STATEMENT-STORE", printedAt = printedAt), sink)
        assertTrue(builder.pageCount > 1)
        assertEquals("All rows must be printed (previously capped at 25)", 60, builder.layoutLog.count { it.kind == "table-row" })
        assertWellFormed(builder, sink)
    }

    @Test fun monthlyReportPaginatesAllDays() {
        val sink = sink("report-monthly")
        val days = (1..31).map { day ->
            val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(2026, 0, day) }
            DailySalesProfit(c.timeInMillis, if (day == 31) 777.25 else day * 10.0, if (day == 31) 444.50 else if (day % 7 == 0) -12.5 else day / 2.0)
        }
        val builder = ReportPdfGenerator.render(
            ReportPdfGenerator.MonthlyReportPdfData("MONTHLY-STORE", 0, 2026, 12345.67, 2345.67, 700.0, 900.0,
                DebtAging(100.0, 200.0, 300.0, 400.0, 1, 2, 3, 4), days, generatedAt = printedAt), sink)
        assertTrue(builder.pageCount > 1)
        assertEquals(31, builder.layoutLog.count { it.kind == "table-row" })
        assertWellFormed(builder, sink)
    }

    @Test fun printFriendlyVariantWithWatermark() {
        val sink = sink("invoice-print-watermark")
        val builder = InvoicePdfGenerator.render(
            InvoicePdfGenerator.InvoicePdfData(
                Transaction(id = 10, customerId = 7, amount = 45.5, isPaid = true, date = printedAt, paymentType = PaymentType.WALLET,
                    paymentMethodName = "WEIGHT-WALLET محفظة الدفع الإلكتروني للمتجر"),
                (1..6).map { item(it, "صنف رقم $it") }, Customer(7, "منى"), "متجر الاختبار",
                options = PdfOptions(printFriendly = true, watermark = "نسخة"), printedAt = printedAt
            ), sink)
        assertWellFormed(builder, sink)
    }

    @Test fun emptyMonthAndManualInvoiceExplainTheirEmptyTables() {
        val report = sink("report-empty")
        val rb = ReportPdfGenerator.render(ReportPdfGenerator.MonthlyReportPdfData("EMPTY-MONTH-STORE", 1, 2026, 0.0, 0.0, 0.0, 0.0,
            DebtAging(0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0), emptyList(), generatedAt = printedAt), report)
        assertEquals(1, rb.layoutLog.count { it.kind == "table-message" })
        val manual = sink("invoice-manual")
        val ib = InvoicePdfGenerator.render(InvoicePdfGenerator.InvoicePdfData(
            Transaction(301, 7, "MANUAL-CUSTOMER", 31.25, isPaid = true, paymentType = PaymentType.BANK, paymentMethodName = "BOUNDARY-BANK",
                note = "MANUAL-PAYMENT-END", date = printedAt, hasItems = false), emptyList(), null, "", printedAt = printedAt), manual)
        assertEquals(1, ib.layoutLog.count { it.kind == "table-message" })
        assertWellFormed(rb, report); assertWellFormed(ib, manual)
    }
}
