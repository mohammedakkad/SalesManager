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
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfFixtureTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private fun write(items:List<InvoiceItem>,customer:Customer?,store:String,name:String):File {
        val generated=InvoicePdfGenerator.generate(context.cacheDir,Transaction(id=8,customerId=7,amount=100.0,isPaid=false,date=0),items,customer,store)
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
}
