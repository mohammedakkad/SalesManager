package com.trader.salesmanager.ui.coverage

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.*
import com.trader.salesmanager.util.export.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ExportStateUiTest:OfflineSellerUiTest() {
    @Test fun loadingBlocksRepeatedExportAndFailureOffersExplicitRetry() {
        val state=MutableStateFlow<ExportState>(ExportState.Idle);var exports=0;var dismiss=0
        show{Column{ExportActionButton(ExportTarget.SALES_REPORT_EXCEL,state.collectAsState().value,{exports++},{dismiss++})}}
        ui.onNode(hasText("تصدير",substring=true)).performClick();assertEquals(1,exports)
        ui.runOnIdle{state.value=ExportState.Loading("تحضير الملف")}
        ui.onNodeWithText("تحضير الملف").performClick();assertEquals(1,exports)
        ui.runOnIdle{state.value=ExportState.Error("تعذر التصدير")}
        ui.onNodeWithText("تعذر التصدير").assertExists();ui.onNodeWithText("إعادة").performClick();assertEquals(2,exports)
    }
    @Test fun actualExcelSuccessOffersSharingDownloadAndCloseWithoutPrint() {
        val file=File(activity.cacheDir,"ui-export.xlsx")
        XlsxWriter().apply{addRow("fixture" to XlsxWriter.CellStyle())}.write(file)
        show{ExportActionButton(ExportTarget.INVENTORY_EXCEL,ExportState.Success(file.name,file.absolutePath,ExportType.XLSX),{}, {})}
        ui.onNodeWithText("جاهز للمشاركة").assertExists();ui.onNodeWithText("ui-export.xlsx").assertExists()
        ui.onNodeWithText("مشاركة").assertExists();ui.onNodeWithText("تنزيل").assertExists();ui.onNodeWithText("طباعة").assertDoesNotExist()
        ui.onNodeWithText("إغلاق").performClick();ui.onNodeWithText("جاهز للمشاركة").assertDoesNotExist();file.delete()
    }
}
