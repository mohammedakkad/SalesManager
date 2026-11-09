package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.Transaction
import com.trader.salesmanager.ui.reports.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class ReportCalendarUiTest:OfflineSellerUiTest() {
    @Test fun selectedDayDetailRoutesCorrectMidnightAndKeepsFullTransactionCount() {
        val vm=mockk<ReportsViewModel>(relaxed=true)
        every{vm.uiState} returns MutableStateFlow(ReportsUiState(isLoading=false,selectedDay=3,calendarMonth=0,calendarYear=2026,
            dayTotals=mapOf(3 to 210.0),selectedDaySummary=Triple(210.0,100.0,110.0),
            selectedDayTransactions=(1..6).map{Transaction(it.toLong(),it.toLong(),"زبون اليوم $it",it*10.0,isPaid=it%2==0)}))
        var selected:Long?=null;show{ReportsScreen({}, {selected=it},viewModel=vm)}
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("زبون اليوم 1"));ui.onNodeWithText("زبون اليوم 1").assertExists()
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("عرض كل العمليات (6)"))
        ui.onNodeWithText("+ 1 عمليات أخرى").assertExists();ui.onNodeWithText("عرض كل العمليات (6)").performScrollTo().performClick()
        val day=Calendar.getInstance().apply{timeInMillis=selected!!};assertEquals(2026,day.get(Calendar.YEAR));assertEquals(0,day.get(Calendar.MONTH));assertEquals(3,day.get(Calendar.DAY_OF_MONTH));assertEquals(0,day.get(Calendar.HOUR_OF_DAY))
    }
    @Test fun calendarMonthArrowsAndDaySelectionUseViewModelActions() {
        val vm=mockk<ReportsViewModel>(relaxed=true);every{vm.uiState} returns MutableStateFlow(ReportsUiState(isLoading=false,calendarMonth=0,calendarYear=2026,dayTotals=mapOf(2 to 50.0)))
        show{ReportsScreen({},viewModel=vm)}
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("يناير"))
        val arrows=ui.onNodeWithText("يناير").onParent().onChildren().filter(hasClickAction())
        arrows[0].performClick();arrows[1].performClick();verify{vm.prevMonth();vm.nextMonth()}
        ui.onNode(hasText("2") and hasClickAction()).performClick();verify{vm.selectDay(2)}
    }
}
