package com.trader.salesmanager.qa

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trader.core.domain.model.Customer
import com.trader.core.domain.model.Transaction
import com.trader.salesmanager.util.export.ExportManager
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/** Actual Android output; completeness is verified separately by PDFBox on these bytes. */
@RunWith(AndroidJUnit4::class)
class LongCustomerStatementTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    @Test fun generateSixtyRowCustomerStatementForActualHostHistoryVerification(){
        val rows=(1..60).map{index->Transaction(id=21000L+index,customerId=700,amount=index*1.25,isPaid=index%2==0,date=index*86400000L,paymentMethodName=if(index%2==0)"CASH" else "")}
        val generated=ExportManager.generateCustomerStatementPdf(context.cacheDir,Customer(700,"LONG-STATEMENT-CUSTOMER",phone="0500000000"),rows,"LONG-STATEMENT-STORE")
        val directory=File(requireNotNull(context.getExternalFilesDir(null)),"qa-pdf").apply{mkdirs()}
        val exported=generated.copyTo(File(directory,"customer-statement-long.pdf"),overwrite=true)
        val bytes=exported.readBytes();assertEquals("%PDF-",bytes.take(5).toByteArray().toString(Charsets.US_ASCII))
        PdfRenderer(ParcelFileDescriptor.open(exported,ParcelFileDescriptor.MODE_READ_ONLY)).use{assertTrue(it.pageCount>=1)}
        fun shell(command:String)=ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)).use{it.readBytes()}
        check(exported.absolutePath.matches(Regex("[A-Za-z0-9_./-]+")))
        val destination="/data/local/tmp/seller-qa-pdf/customer-statement-long.pdf"
        shell("/system/bin/mkdir -p /data/local/tmp/seller-qa-pdf");shell("/system/bin/cp ${exported.absolutePath} $destination")
        assertArrayEquals(bytes,shell("/system/bin/cat $destination"))
        val digest=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt() and 255)}
        Log.i("SellerQaPdf","package=${context.packageName} source=${exported.absolutePath} exported=$destination sha256=$digest")
        assertTrue(generated.delete())
    }
}
