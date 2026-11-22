package com.trader.salesmanager.util.export

import android.app.Application
import com.trader.core.domain.model.*
import com.trader.salesmanager.util.pdf.ReportPdfGenerator
import androidx.lifecycle.viewModelScope
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class ExportLifecycleContractsTest {
    private lateinit var vm:ExportViewModel
    private lateinit var directory:File
    @Before fun setup(){Dispatchers.setMain(StandardTestDispatcher());vm=ExportViewModel();directory=Files.createTempDirectory("export-lifecycle").toFile();mockkObject(ExportManager)}
    @After fun cleanup(){vm.viewModelScope.cancel();unmockkObject(ExportManager);directory.deleteRecursively();Dispatchers.resetMain()}
    @Test fun exportClaimsLoadingSynchronouslyBeforeAnotherTapCanQueueWork()=runTest {
        vm.exportInventoryExcel(emptyList(),"offline fixture",directory)
        assertTrue("A second tap must see the in-progress export",vm.state.value is ExportState.Loading)
        vm.viewModelScope.cancel()
    }
    @Test fun generatorCancellationRemainsCancellationRatherThanAnError()=runTest {
        every { ExportManager.generateInventoryExcel(any(),any(),any()) } throws CancellationException("offline cancellation fixture")
        vm.exportInventoryExcel(emptyList(),"fixture",directory);runCurrent()
        val state=withContext(Dispatchers.Default) { withTimeout(10000) { vm.state.first { it !is ExportState.Loading } } }
        assertEquals(ExportState.Idle,state)
    }
    @Test fun resetDuringWriteCannotResurrectAStaleSuccess()=runTest {
        val began=CompletableDeferred<Unit>();val release=CountDownLatch(1)
        every { ExportManager.generateInventoryExcel(any(),any(),any()) } answers {
            began.complete(Unit)
            check(release.await(5,TimeUnit.SECONDS)) { "test release timed out" }
            callOriginal()
        }
        try {
            vm.exportInventoryExcel(emptyList(),"fixture",directory);runCurrent()
            withContext(Dispatchers.Default){withTimeout(10000){began.await()}}
            val work=vm.viewModelScope.coroutineContext[Job]!!.children.toList()
            vm.reset();assertEquals(ExportState.Idle,vm.state.value)
            release.countDown();work.joinAll();runCurrent()
            assertEquals("A dismissed export must remain dismissed",ExportState.Idle,vm.state.value)
        } finally {release.countDown()}
    }

    private fun assertOriginalCause(expected:Throwable, actual:Throwable?) {
        assertNotNull(actual)
        assertEquals(expected.javaClass,actual!!.javaClass)
        assertEquals(expected.message,actual.message)
        // Coroutine stack recovery can copy an exception while retaining its original cause.
        assertTrue(generateSequence(actual) { it.cause }.any { it === expected })
    }
    private suspend fun completed():ExportState = withContext(Dispatchers.Default) {
        withTimeout(10000){vm.state.first { it !is ExportState.Loading }}
    }
    @Test fun rapidRepeatedRequestsProduceOneRealWorkbookAndTypedSuccess()=runTest {
        val count=AtomicInteger()
        every { ExportManager.generateInventoryExcel(any(),any(),any()) } answers {count.incrementAndGet();callOriginal()}
        vm.exportInventoryExcel(emptyList(),"fixture",directory)
        vm.exportInventoryExcel(emptyList(),"fixture",directory)
        val result=completed() as ExportState.Success
        vm.viewModelScope.coroutineContext[Job]!!.children.toList().joinAll()
        assertEquals(1,count.get());assertEquals(ExportType.XLSX,result.type)
        assertEquals("تقرير_المخزون.xlsx",result.fileName)
        ZipFile(result.filePath).use { assertNotNull(it.getEntry("xl/workbook.xml"));assertNotNull(it.getEntry("xl/worksheets/sheet1.xml")) }
    }
    @Test fun ioFailureRetainsCauseAndDismissAllowsSuccessfulRetry()=runTest {
        val error=IOException("disk fixture")
        every { ExportManager.generateInventoryExcel(any(),any(),any()) } throws error
        vm.exportInventoryExcel(emptyList(),"fixture",directory)
        val result=completed() as ExportState.Error
        assertOriginalCause(error,result.cause);assertTrue(result.message.contains("خطأ في الكتابة"))
        vm.dismissError();assertEquals(ExportState.Idle,vm.state.value)
        every { ExportManager.generateInventoryExcel(any(),any(),any()) } answers {callOriginal()}
        vm.exportInventoryExcel(emptyList(),"fixture",directory)
        assertTrue(completed() is ExportState.Success)
    }
    @Test fun memoryFailureDuringCustomerStatementIsVisibleWithoutSuccessOrAFakePdf()=runTest {
        val customer=Customer(1,"fixture")
        val error=OutOfMemoryError("controlled fixture, no actual heap exhaustion")
        every { ExportManager.generateCustomerStatementPdf(directory,customer,emptyList(),"fixture") } throws error
        vm.exportCustomerStatementPdf(customer,emptyList(),"fixture",directory)
        val result=completed() as ExportState.Error
        assertEquals("لا توجد ذاكرة كافية",result.message);assertOriginalCause(error,result.cause)
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }
    @Test fun salesExportDelegatesToRealWorkbookAndPreservesItsPublicFileName()=runTest {
        vm.exportSalesReportExcel(listOf(Transaction(id=1,customerId=1,amount=15.5,isPaid=true)),
            "fixture period","fixture store",emptyList(),emptyList(),emptyList(),directory)
        val result=completed() as ExportState.Success
        assertEquals("تقرير_المبيعات.xlsx",result.fileName);assertEquals(ExportType.XLSX,result.type)
        ZipFile(result.filePath).use { assertNotNull(it.getEntry("xl/sharedStrings.xml")) }
    }
    @Test fun invoiceAndMonthlyEntryPointsPreserveGeneratorFailureAndTheirPublicNames()=runTest {
        val error=IOException("controlled output failure")
        every { ExportManager.generateInvoicePdf(any(),any(),any(),any(),any(),any(),any(),any(),any()) } throws error
        vm.exportInvoicePdf(Transaction(id=7,customerId=1,amount=10.0,isPaid=false),emptyList(),"fixture",cacheDir=directory)
        assertTrue((vm.state.value as ExportState.Loading).message.contains("فاتورة_7.pdf"))
        assertOriginalCause(error,(completed() as ExportState.Error).cause)
        vm.reset()
        val data=ReportPdfGenerator.MonthlyReportPdfData("fixture",0,2026,0.0,0.0,0.0,0.0,DebtAging(0.0,0.0,0.0,0.0,0,0,0,0),emptyList())
        every { ExportManager.generateMonthlyReportPdf(directory,data) } throws error
        vm.exportMonthlyReportPdf(data,directory)
        assertTrue((vm.state.value as ExportState.Loading).message.contains("تقرير_2026_1.pdf"))
        assertOriginalCause(error,(completed() as ExportState.Error).cause)
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }

    @Test fun timedOutGeneratorRemainsCancellationAndAllowsNewExport()=runTest {
        every { ExportManager.generateInventoryExcel(any(),any(),any()) } answers {
            runBlocking { withTimeout(10) { awaitCancellation() } }
        }
        vm.exportInventoryExcel(emptyList(),"fixture",directory)
        assertEquals(ExportState.Idle,completed())
        every { ExportManager.generateInventoryExcel(any(),any(),any()) } answers {callOriginal()}
        vm.exportInventoryExcel(emptyList(),"fixture",directory)
        assertTrue(completed() is ExportState.Success)
    }
}
