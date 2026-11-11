package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.salesmanager.ui.reports.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReportSummaryPresentationUiTest:OfflineSellerUiTest() {
    private val vm=mockk<ReportsViewModel>(relaxed=true)
    private val state=MutableStateFlow(ReportsUiState(isLoading=false,totalAmount=300.0,paidAmount=100.0,unpaidAmount=200.0,txCount=2))
    private fun report(){every{vm.uiState} returns state;show{ReportsScreen({},viewModel=vm)};ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("إجمالي المبيعات"))}
    @Test fun summaryShowsTheSelectedPeriodsActualTransactionCount(){report();ui.onNodeWithText("2 عملية").assertIsDisplayed()}
    @Test fun summarySeparatesPaidAndPendingValuesFromTheSamePeriod(){report();ui.onNodeWithText("معلق").assertIsDisplayed();ui.onNodeWithText("₪ 100").assertIsDisplayed();ui.onNodeWithText("₪ 200").assertIsDisplayed()}
    @Test fun changedPeriodReplacesCountAndBothAmountsTogether(){
        report();state.value=state.value.copy(totalAmount=150.0,paidAmount=75.0,unpaidAmount=75.0,txCount=1)
        ui.onNodeWithText("1 عملية").assertIsDisplayed();ui.onAllNodesWithText("₪ 75").assertCountEquals(2);ui.onNodeWithText("2 عملية").assertDoesNotExist()
    }
    @Test fun zeroPeriodDoesNotInventPaymentPillsOrATransactionCount(){
        state.value=ReportsUiState(isLoading=false);report();ui.onNodeWithText("معلق").assertDoesNotExist();ui.onNodeWithText("0 عملية").assertDoesNotExist()
    }
}
