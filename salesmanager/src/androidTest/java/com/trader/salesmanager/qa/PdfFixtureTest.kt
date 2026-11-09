package com.trader.salesmanager.qa

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import java.security.MessageDigest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trader.core.domain.model.*
import com.trader.salesmanager.util.pdf.InvoicePdfGenerator
import com.trader.salesmanager.util.pdf.ReportPdfGenerator
import com.trader.salesmanager.util.export.ExportManager
import java.util.Calendar
import java.util.TimeZone
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfFixtureTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private fun write(items:List<InvoiceItem>,customer:Customer?,store:String,name:String):File {
        val generated=InvoicePdfGenerator.generate(context.cacheDir,Transaction(id=8,customerId=7,amount=100.0,isPaid=false,date=0),items,customer,store)
        return export(generated,name)
    }
    private fun export(generated:File,name:String):File {
        val directory=File(requireNotNull(context.getExternalFilesDir(null)),"qa-pdf").apply { mkdirs() }
        return generated.copyTo(File(directory,name),overwrite=true).also { exported ->
            assertTrue("Missing Android-generated PDF: ${exported.absolutePath}", exported.isFile && exported.length() > 0)
            // Export before connectedAndroidTest can uninstall the target APK and remove
            // its app-specific external storage. Shell owns this independent directory.
            val destination = "/data/local/tmp/seller-qa-pdf/$name"
            fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
            ).bufferedReader().use { it.readText() }
            // executeShellCommand tokenizes arguments; it is not a shell interpreter.
            check(exported.absolutePath.matches(Regex("[A-Za-z0-9_./-]+")))
            shell("mkdir -p /data/local/tmp/seller-qa-pdf")
            shell("cp ${exported.absolutePath} $destination")
            val result = shell("sha256sum $destination")
            val digest = MessageDigest.getInstance("SHA-256").digest(exported.readBytes())
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            assertEquals("Shell export failed: $result", digest, result.trim().substringBefore(' '))
            Log.i("SellerQaPdf", "package=${context.packageName} source=${exported.absolutePath} exported=$destination sha256=$digest")
            generated.delete()
        }
    }
    @Test fun generateSingleInvoiceForHostContentVerification() {
        val file=write(listOf(InvoiceItem(id="i",productName="صنف",quantity=4.0,pricePerUnit=25.0,totalPrice=100.0)),Customer(id=7,name="أحمد"),"متجر الاختبار","invoice-content.pdf")
        assertEquals("%PDF-",file.readBytes().take(5).toByteArray().toString(Charsets.US_ASCII))
        PdfRenderer(ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)).use { assertEquals(1,it.pageCount) }
    }
    @Test fun generateLongInvoiceForHostPaginationAndContentVerification() {
        val file=write((0..99).map { InvoiceItem(id="item$it",productName="ITEM-${it.toString().padStart(3,'0')}",quantity=1.0,pricePerUnit=1.0,totalPrice=1.0) },null,"fixture","invoice-long.pdf")
        PdfRenderer(ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)).use { assertTrue(it.pageCount>1) }
    }
    @Test fun generateCustomerStatementForHostBalanceVerification() {
        val transactions=listOf(Transaction(id=201,customerId=7,amount=20.0,isPaid=true,date=0,paymentMethodName="CASH"),Transaction(id=202,customerId=7,amount=30.0,isPaid=false,date=1))
        val file=export(ExportManager.generateCustomerStatementPdf(context.cacheDir,Customer(7,"CUSTOMER-STATEMENT",phone="0500000000"),transactions,"STATEMENT-STORE"),"customer-statement.pdf")
        assertEquals("%PDF-",file.readBytes().take(5).toByteArray().toString(Charsets.US_ASCII))
        PdfRenderer(ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)).use { assertEquals(1,it.pageCount) }
    }
    @Test fun generateMonthlyReportForHostPaginationAndFinancialVerification() {
        val days=(1..31).map { day ->
            val calendar=Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply{clear();set(2026,0,day)}
            DailySalesProfit(calendar.timeInMillis,if(day==31)777.25 else day.toDouble(),if(day==31)444.50 else day/2.0)
        }
        val data=ReportPdfGenerator.MonthlyReportPdfData("MONTHLY-STORE",0,2026,12345.67,2345.67,700.0,900.0,
            DebtAging(100.0,200.0,300.0,400.0,1,2,3,4),days,generatedAt=1767225600000)
        val file=export(ReportPdfGenerator.generate(context.cacheDir,data),"monthly-report.pdf")
        PdfRenderer(ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)).use { assertTrue("All daily rows must paginate",it.pageCount>1) }
    }

}
