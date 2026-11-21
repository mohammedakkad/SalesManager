package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.reports.*
import com.trader.salesmanager.ui.customers.details.*
import com.trader.salesmanager.ui.transactions.details.*
import com.trader.salesmanager.ui.inventory.list.*
import com.trader.salesmanager.util.export.*
import com.trader.salesmanager.util.pdf.ReportPdfGenerator.MonthlyReportPdfData
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import java.io.File

/** Actual Compose actions and real XLSX source bytes. PDF tests verify delegation, never create stand-in PDFs. */
@RunWith(RobolectricTestRunner::class)
class ReportExportJourneysUiTest:OfflineSellerUiTest() {
    private val reports=mockk<ReportsViewModel>(relaxed=true)
    private val export=mockk<ExportViewModel>(relaxed=true)
    private val out=MutableStateFlow<ExportState>(ExportState.Idle)
    private val rows=listOf(Transaction(7,3,"زبون التقرير",125.0,isPaid=true))
    private val state=MutableStateFlow(ReportsUiState(isLoading=false,filteredTransactions=rows,period=ReportPeriod.WEEK,
        dailySales=listOf(DaySalesEntry("03/01",125.0,125.0)),paymentShares=listOf(PaymentShare("نقد",125.0)),
        topSpenders=listOf(CustomerRank("زبون التقرير",125.0)),calendarMonth=0,calendarYear=2026,
        monthlyReportMonth=0,monthlyReportYear=2026,isMonthlyReportLoading=false))
    private val monthly=MonthlyReportPdfData("متجر الاختبار",0,2026,125.0,25.0,200.0,350.0,DebtAging(),emptyList(),1)
    private lateinit var file:File
    @Before fun fixture(){
        ShadowToast.reset();every{reports.uiState} returns state;every{reports.buildMonthlyReportData(any())} returns monthly
        every{export.state} returns out;loadKoinModules(module{viewModel{export}})
        file=File(activity.cacheDir,"report-journey.xlsx");XlsxWriter().apply{addRow("تقرير حقيقي" to XlsxWriter.CellStyle())}.write(file)
        mockkObject(ExportManager)
    }
    @After fun cleanup(){unmockkObject(ExportManager);file.delete()}
    private fun report(){show{ReportsScreen({},viewModel=reports)}}
    private fun successfulFile(){out.value=ExportState.Success(file.name,file.absolutePath,ExportType.XLSX)}
    private fun retryDownload(sheetTitle:String="الملف جاهز",hasReset:Boolean=true){
        val original=file.readBytes()
        every{ExportManager.saveToDownloads(any(),file,file.name)} returnsMany listOf(false,true)
        ui.onNodeWithText("تنزيل").performClick()
        ui.onNodeWithText(sheetTitle).assertExists();ui.onNodeWithText(file.name).assertExists()
        assertEquals("تعذر حفظ الملف. حاول مرة أخرى.",ShadowToast.getTextOfLatestToast())
        verify(exactly=0){export.reset()}
        ui.onNodeWithText("تنزيل").performClick();ui.onNodeWithText(sheetTitle).assertDoesNotExist()
        verify(exactly=2){ExportManager.saveToDownloads(any(),file,file.name)}
        verify(exactly=if(hasReset)1 else 0){export.reset()};assertArrayEquals(original,file.readBytes())
    }
    @Test fun excelActionPassesTheSelectedPeriodTransactionsAndAnalytics(){
        report();ui.onNodeWithContentDescription("تصدير Excel").performClick()
        val s=state.value
        verify(exactly=1){export.exportSalesReportExcel(rows,"WEEK",any(),s.dailySales,s.topSpenders,s.paymentShares,activity.cacheDir)}
    }
    @Test fun activeExportDisablesMonthlyShareAndPrintAndCannotQueueAnotherExcel(){
        out.value=ExportState.Loading("تحضير الملف");report()
        ui.onNodeWithText("مشاركة PDF").assertIsNotEnabled();ui.onNodeWithText("طباعة").assertIsNotEnabled()
        ui.onNodeWithContentDescription("تصدير Excel").assertDoesNotExist()
        verify(exactly=0){export.exportSalesReportExcel(any(),any(),any(),any(),any(),any(),any())}
    }
    @Test fun monthlyShareDelegatesTheReadySnapshotAndCacheDirectory(){
        report();ui.onNodeWithText("مشاركة PDF").performClick()
        verify(exactly=1){reports.buildMonthlyReportData(any())};verify(exactly=1){export.exportMonthlyReportPdf(monthly,activity.cacheDir)}
    }
    @Test fun monthlyPrintRequestsTheSameFinancialSnapshotBeforePrinting(){
        report();ui.onNodeWithText("طباعة").performClick()
        verify(exactly=1){reports.buildMonthlyReportData(any())};verify(exactly=1){export.exportMonthlyReportPdf(monthly,activity.cacheDir)}
    }
    @Test fun generationFailureDisplaysItsErrorAndDismissesTheFailedState(){
        out.value=ExportState.Error("تعذر إنشاء التقرير");report();ui.waitForIdle()
        assertEquals("تعذر إنشاء التقرير",ShadowToast.getTextOfLatestToast());verify(exactly=1){export.dismissError()}
        ui.onNodeWithText("الملف جاهز").assertDoesNotExist();verify(exactly=0){export.reset()}
    }
    @Test fun closingAnExcelResultResetsOnceWithoutOfferingPdfPrinting(){
        successfulFile();report();ui.onNodeWithText(file.name).assertExists()
        ui.onNodeWithText("إغلاق").performClick();ui.onNodeWithText("الملف جاهز").assertDoesNotExist()
        verify(exactly=1){export.reset()};verify(exactly=0){ExportManager.saveToDownloads(any(),any(),any())}
    }
    @Test fun reportShareUsesTheActualWorkbookAndMimeTypeThenResets(){
        successfulFile();every{ExportManager.shareFile(any(),file,ExportType.XLSX.mimeType)} just Runs
        report();ui.onNodeWithText("مشاركة",useUnmergedTree=true).performClick()
        verify(exactly=1){ExportManager.shareFile(any(),file,ExportType.XLSX.mimeType)};verify(exactly=1){export.reset()}
        ui.onNodeWithText("الملف جاهز").assertDoesNotExist()
    }
    @Test fun reportWhatsAppUsesTheActualWorkbookAndMimeTypeThenResets(){
        successfulFile();every{ExportManager.shareToWhatsApp(any(),file,ExportType.XLSX.mimeType)} just Runs
        report();ui.onNodeWithText("واتساب").performClick()
        verify(exactly=1){ExportManager.shareToWhatsApp(any(),file,ExportType.XLSX.mimeType)};verify(exactly=1){export.reset()}
    }
    @Test fun failedReportDownloadKeepsTheFileAvailableForAnExplicitRetry(){successfulFile();report();retryDownload()}
    @Test fun successfulReportDownloadClosesTheSheetAndResetsOnlyOnce(){
        successfulFile();every{ExportManager.saveToDownloads(any(),file,file.name)} returns true
        report();ui.onNodeWithText("تنزيل").performClick();ui.onNodeWithText("الملف جاهز").assertDoesNotExist()
        verify(exactly=1){export.reset()};verify(exactly=1){ExportManager.saveToDownloads(any(),file,file.name)}
    }
    @Test fun sharedExportButtonKeepsAFailedWorkbookDownloadRetryable(){
        successfulFile();show{ExportActionButton(ExportTarget.INVENTORY_EXCEL,out.value,{}, {})};retryDownload("جاهز للمشاركة",false)
    }
    @Test fun sharedExportButtonClosesOnlyAfterASuccessfulDownload(){
        successfulFile();every{ExportManager.saveToDownloads(any(),file,file.name)} returns true
        show{ExportActionButton(ExportTarget.INVENTORY_EXCEL,out.value,{}, {})};ui.onNodeWithText("تنزيل").performClick()
        ui.onNodeWithText("جاهز للمشاركة").assertDoesNotExist();verify(exactly=1){ExportManager.saveToDownloads(any(),file,file.name)}
    }
    @Test fun customerStatementDownloadFailureRetainsItsExportForRetry(){
        successfulFile();val vm=mockk<CustomerDetailsViewModel>(relaxed=true)
        every{vm.uiState} returns MutableStateFlow(CustomerDetailsUiState(customer=Customer(3,"زبون التقرير"),isLoading=false))
        show{CustomerDetailsScreen(3,{},{},{},{},viewModel=vm)};retryDownload()
    }
    @Test fun invoiceDownloadFailureRetainsItsExportForRetry(){
        successfulFile();val vm=mockk<TransactionDetailsViewModel>(relaxed=true)
        every{vm.uiState} returns MutableStateFlow(TransactionDetailsUiState(transaction=rows.single(),isItemsLoaded=true))
        show{TransactionDetailsScreen(7,{},{},{},viewModel=vm)};retryDownload()
    }
    @Test fun inventoryDownloadFailureRetainsItsExportForRetry(){
        successfulFile();val vm=mockk<InventoryListViewModel>(relaxed=true)
        every{vm.uiState} returns MutableStateFlow(InventoryListUiState(isLoading=false))
        show{InventoryListScreen({},{},{},{},viewModel=vm)};retryDownload()
    }
}
