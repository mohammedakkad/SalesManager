package com.trader.salesmanager.qa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trader.core.domain.model.*
import com.trader.salesmanager.util.pdf.InvoicePdfGenerator
import com.trader.salesmanager.util.pdf.ReportPdfGenerator
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/** Offline business inputs rendered by actual Android PdfDocument, never host substitutes. */
@RunWith(AndroidJUnit4::class)
class FinancialPdfBoundaryTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private fun export(generated:File,name:String){
        val directory=File(requireNotNull(context.getExternalFilesDir(null)),"qa-pdf").apply{mkdirs()}
        val exported=generated.copyTo(File(directory,name),overwrite=true)
        val bytes=exported.readBytes();assertEquals("%PDF-",bytes.take(5).toByteArray().toString(Charsets.US_ASCII))
        PdfRenderer(ParcelFileDescriptor.open(exported,ParcelFileDescriptor.MODE_READ_ONLY)).use{assertTrue(it.pageCount>=1)}
        fun shell(command:String)=ParcelFileDescriptor.AutoCloseInputStream(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)).use{it.readBytes()}
        check(exported.absolutePath.matches(Regex("[A-Za-z0-9_./-]+")))
        val destination="/data/local/tmp/seller-qa-pdf/$name"
        shell("/system/bin/mkdir -p /data/local/tmp/seller-qa-pdf");shell("/system/bin/cp ${exported.absolutePath} $destination")
        assertArrayEquals(bytes,shell("/system/bin/cat $destination"))
        val digest=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it.toInt() and 255)}
        Log.i("SellerQaPdf","package=${context.packageName} source=${exported.absolutePath} exported=$destination sha256=$digest")
        assertTrue(generated.delete())
    }
    @Test fun generatePaidManualBankInvoiceWithoutItemsForActualHostVerification(){
        val sale=Transaction(301,7,"MANUAL-CUSTOMER",31.25,isPaid=true,paymentType=PaymentType.BANK,
            paymentMethodName="BOUNDARY-BANK",note="MANUAL-PAYMENT-END",date=0,hasItems=false)
        export(InvoicePdfGenerator.generate(context.cacheDir,sale,emptyList(),null,""),"invoice-manual-bank.pdf")
    }
    @Test fun generateFullyReturnedCashInvoiceWithZeroNetForActualHostVerification(){
        val sale=Transaction(302,7,"FULL-RETURN-CUSTOMER",0.0,originalAmount=80.0,isPaid=true,paymentType=PaymentType.CASH,
            paymentMethodName="RETURN-CASH",note="FULL-RETURN-END",date=0,hasItems=true,returnStatus=TransactionReturnStatus.FULLY_RETURNED)
        val item=InvoiceItem("full-line",302,"full-product","FULL-RETURN-ITEM","full-unit","PIECE",4.0,20.0,80.0)
        export(InvoicePdfGenerator.generate(context.cacheDir,sale,listOf(item),Customer(7,"FULL-RETURN-CUSTOMER"),"FULL-RETURN-STORE",
            returnSummary=ReturnSummary(TransactionReturnStatus.FULLY_RETURNED,80.0,mapOf("full-unit" to 4.0))),"invoice-full-return.pdf")
    }
    @Test fun generateFractionalWeightReturnWithLogoForActualHostVerification(){
        val sale=Transaction(303,7,"FRACTIONAL-CUSTOMER",33.75,originalAmount=39.375,isPaid=false,paymentType=PaymentType.WALLET,
            paymentMethodName="WEIGHT-WALLET",date=0,hasItems=true,returnStatus=TransactionReturnStatus.PARTIALLY_RETURNED)
        val item=InvoiceItem("weight-line",303,"weight-product","FRACTIONAL-KG","weight-unit","KG",2.625,15.0,39.375)
        val logo=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.BLUE)}
        try {
            export(InvoicePdfGenerator.generate(context.cacheDir,sale,listOf(item),Customer(7,"FRACTIONAL-CUSTOMER"),"WEIGHT-STORE",logo=logo,
                returnSummary=ReturnSummary(TransactionReturnStatus.PARTIALLY_RETURNED,5.625,mapOf("weight-unit" to .375))),"invoice-fractional-logo.pdf")
        }finally{logo.recycle()}
    }
    @Test fun generateEmptyMonthWithoutInventingFinancialActivityForActualHostVerification(){
        val report=ReportPdfGenerator.MonthlyReportPdfData("EMPTY-MONTH-STORE",1,2026,0.0,0.0,0.0,0.0,
            DebtAging(0.0,0.0,0.0,0.0,0,0,0,0),emptyList(),generatedAt=1767225600000)
        export(ReportPdfGenerator.generate(context.cacheDir,report),"monthly-report-empty.pdf")
    }
}
