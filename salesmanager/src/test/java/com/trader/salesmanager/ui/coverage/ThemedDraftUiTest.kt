package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.customers.addedit.*
import com.trader.salesmanager.ui.inventory.addedit.*
import com.trader.salesmanager.ui.returns.*
import com.trader.salesmanager.ui.reports.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers="w480dp-h900dp")
class ThemedDraftUiTest:AppThemeUiFixture() {
    @Test fun customerDraftSurvivesTwoThemeChangesAndOnlyExplicitSavePersistsIt(){
        val repo=mockk<CustomerRepository>(relaxed=true);coEvery{repo.getPhoneConflict(any(),any())} returns null
        val vm=owned(AddEditCustomerViewModel(repo));var back=0
        showApp{AddEditCustomerScreen(null,{back++},vm)}
        ui.onNodeWithText("اسم الزبون *").performTextInput("  زبون المظهر  ")
        dark(true);ui.onNodeWithText("  زبون المظهر  ").assertExists();coVerify(exactly=0){repo.insertCustomer(any())}
        dark(false)
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("إضافة الزبون"))
        ui.onNodeWithText("إضافة الزبون").assertIsEnabled().performClick()
        ui.waitUntil(3000){back==1};coVerify(exactly=1){repo.insertCustomer(match{it.name=="زبون المظهر"})}
    }
    @Test fun productDraftKeepsUnitCostsAndStockWithoutSavingOnThemeChange(){
        val repo=mockk<ProductRepository>(relaxed=true);coEvery{repo.saveProduct(any(),any(),any())} returns "saved"
        val vm=owned(AddEditProductViewModel(repo));var back=0
        showApp{AddEditProductScreen(onNavigateUp={back++},viewModel=vm)}
        ui.onNode(hasSetTextAction() and hasText("اسم الصنف *")).performTextReplacement("قهوة المظهر")
        ui.runOnIdle{vm.setCategory("غذاء");vm.updateUnitPrice(0,"25.5");vm.updateUnitCostPrice(0,"15.25");vm.updateUnitQty(0,"4.5")}
        dark(true);dark(false)
        coVerify(exactly=0){repo.saveProduct(any(),any(),any())}
        ui.onNodeWithText("إضافة الصنف").performScrollTo().performClick()
        coVerify(exactly=1){repo.saveProduct(match{it.name=="قهوة المظهر"},match{it.single().price==25.5&&it.single().costPrice==15.25&&it.single().quantityInStock==4.5},any())}
        ui.waitUntil(3000){back==1}
    }
    @Test fun pendingReturnKeepsCancellationDisabledWhileThemeAndFailureStateChange(){
        val item=InvoiceItem("line",1,quantity=2.0,pricePerUnit=25.0)
        val vm=mockk<ReturnViewModel>(relaxed=true)
        val state=MutableStateFlow(ReturnScreenState(isLoading=false,lines=listOf(ReturnLineState(item,"صنف الإرجاع","حبة",returnQty=1.0,isSelected=true)),showConfirmSheet=true,processingState=ReturnUiState.Loading))
        every{vm.state} returns state
        showApp{ReturnProcessScreen(1,{},{},viewModel=vm)}
        dark(true);ui.onNodeWithText("إلغاء").assertIsNotEnabled()
        verify(exactly=0){vm.confirmReturn()}
        ui.runOnIdle{state.value=state.value.copy(processingState=ReturnUiState.Error("فشل محفوظ"))}
        ui.onNodeWithText("فشل محفوظ").assertExists()
        dark(false);ui.onNodeWithText("إلغاء").assertIsEnabled()
        assertEquals(25.0,state.value.totalRefund,0.0)
    }
    @Test fun reportSelectionAndReadyMonthRemainStableWhenThemeChanges(){
        val vm=mockk<ReportsViewModel>(relaxed=true)
        val state=MutableStateFlow(ReportsUiState(isLoading=false,totalAmount=300.0,paidAmount=100.0,unpaidAmount=200.0,txCount=2,
            calendarYear=2026,calendarMonth=0,monthlyReportYear=2026,monthlyReportMonth=0,isMonthlyReportLoading=false,monthlyReportTotals=FinancialReportTotals(300.0,50.0),selectedDay=3))
        every{vm.uiState} returns state
        showApp{ReportsScreen({},viewModel=vm)}
        ui.onNodeWithText("مشاركة PDF").assertIsEnabled()
        dark(true);ui.onNodeWithText("مشاركة PDF").assertIsEnabled()
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("لا توجد عمليات في هذا اليوم"))
        ui.onNodeWithText("لا توجد عمليات في هذا اليوم").assertExists()
        dark(false);ui.onNodeWithText("لا توجد عمليات في هذا اليوم").assertExists()
        verify(exactly=0){vm.prevMonth()};verify(exactly=0){vm.nextMonth()};verify(exactly=0){vm.setPeriod(any())}
    }
}
