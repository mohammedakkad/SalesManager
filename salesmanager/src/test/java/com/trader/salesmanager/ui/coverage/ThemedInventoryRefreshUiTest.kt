package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.inventory.session.*
import com.trader.salesmanager.ui.inventory.detail.*
import com.trader.salesmanager.ui.inventory.reports.*
import io.mockk.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Live stock refreshes must update the displayed counts without issuing inventory writes. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers="w480dp-h900dp")
class ThemedInventoryRefreshUiTest:AppThemeUiFixture() {
    private val vm=mockk<InventorySessionViewModel>(relaxed=true)
    private val state=MutableStateFlow(InventorySessionUiState(activeSession=InventorySession("s","fixture"),isLoading=false))
    private val row=InventorySessionItem("i","s","p","أرز حي","u","كيس حي",5.0)
    private val rows=MutableStateFlow(listOf(row))
    @Before fun session(){
        every{vm.uiState} returns state;every{vm.sessionItems} returns rows
        every{vm.totalItems} answers{rows.value.size};every{vm.countedItems} answers{rows.value.count{it.actualQuantity!=null}}
        every{vm.progressPercent} answers{vm.countedItems.toFloat()/vm.totalItems.coerceAtLeast(1)}
        every{vm.adjustmentsCount} answers{rows.value.count{it.actualQuantity!=null&&kotlin.math.abs(it.difference)>.001}}
    }
    @Test fun liveCountsReplacePendingRowsAndNeverApplyTheInventoryAutomatically(){
        showApp{InventorySessionScreen({},vm)}
        ui.onNodeWithText("التقدم: 0 / 1").assertExists();dark(true)
        ui.runOnIdle{rows.value=listOf(row.copy(actualQuantity=7.0))}
        ui.onNodeWithText("التقدم: 1 / 1").assertExists()
        ui.onNodeWithText("الفعلي: 7").assertExists();ui.onNodeWithText("+2").assertExists()
        ui.runOnIdle{rows.value=listOf(row.copy(actualQuantity=3.0))}
        ui.onNodeWithText("الفعلي: 3").assertExists();ui.onNodeWithText("-2").assertExists()
        dark(false)
        ui.runOnIdle{rows.value=listOf(row.copy(actualQuantity=5.0))}
        ui.onNodeWithText("+2").assertDoesNotExist();ui.onNodeWithText("-2").assertDoesNotExist()
        verify(exactly=0){vm.finishSession()};verify(exactly=0){vm.updateItemCount(any(),any())}
    }
    @Test fun pendingFilterShowsCompletionThenReopensWhenACountIsClearedExternally(){
        state.value=state.value.copy(showOnlyPending=true)
        showApp{InventorySessionScreen({},vm)};dark(true)
        ui.runOnIdle{rows.value=listOf(row.copy(actualQuantity=5.0))}
        ui.onNodeWithText("جميع الأصناف تم عدّها ✅").assertExists()
        ui.onNodeWithText(row.unitLabel).assertDoesNotExist()
        dark(false);ui.runOnIdle{rows.value=listOf(row)}
        ui.onNodeWithText("جميع الأصناف تم عدّها ✅").assertDoesNotExist()
        ui.onNodeWithText(row.unitLabel).assertExists()
        ui.onNodeWithContentDescription("عرض جميع الأصناف").assertExists()
        verify(exactly=0){vm.clearItemCount(any())}
    }
    @Test fun finishingStateBlocksTheExistingConfirmationUntilTheCommandCanBeSubmitted(){
        showApp{InventorySessionScreen({},vm)}
        ui.onNodeWithText("إنهاء الجرد وتطبيق التعديلات").performClick();dark(true)
        ui.onNodeWithText("⚠️ الأصناف غير المعدودة لن يتم تعديل كمياتها.").assertExists()
        ui.runOnIdle{state.value=state.value.copy(isFinishing=true)}
        ui.onNodeWithText("إنهاء الجرد وتطبيق التعديلات").assertIsNotEnabled()
        ui.onNode(hasClickAction() and hasAnyAncestor(isDialog()) and isEnabled()).assertExists()
        verify(exactly=0){vm.finishSession()}
        dark(false);ui.runOnIdle{state.value=state.value.copy(isFinishing=false)}
        ui.onNodeWithText("تطبيق وإنهاء").assertIsEnabled().performClick()
        verify(exactly=1){vm.finishSession()};ui.onNode(isDialog()).assertDoesNotExist()
    }
    @Test fun stockReportKeepsTheAlertTabAndRemovesARecoveredProductsWarning(){
        val report=mockk<StockReportsViewModel>(relaxed=true)
        val product=ProductWithUnits(Product(id="p",name="نفاد حي"),listOf(ProductUnit(id="u",productId="p",quantityInStock=0.0)))
        val live=MutableStateFlow(StockReportsUiState(isLoading=false,outOfStockItems=listOf(product)))
        every{report.uiState} returns live
        showApp{StockReportsScreen({},viewModel=report)}
        ui.onNodeWithText("تنبيهات").performClick();ui.onNodeWithText("نفاد حي").assertExists();dark(true)
        ui.runOnIdle{live.value=live.value.copy(outOfStockItems=emptyList())}
        ui.onNodeWithText("كل الأصناف بكميات كافية ✅").assertExists();ui.onNodeWithText("نفاد حي").assertDoesNotExist()
        dark(false);ui.onNodeWithText("كل الأصناف بكميات كافية ✅").assertExists()
    }
    @Test fun stockReportReplacesUnitTypesAndQuantitiesWithoutKeepingTheOldRow(){
        val report=mockk<StockReportsViewModel>(relaxed=true)
        val live=MutableStateFlow(StockReportsUiState(isLoading=false,stockItems=listOf(StockReportItem("u","وزن قديم","كيلو",1.25,UnitType.WEIGHT,0.0,0.0,0))))
        every{report.uiState} returns live
        showApp{StockReportsScreen({},viewModel=report)}
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("وزن قديم"));dark(true)
        ui.runOnIdle{live.value=live.value.copy(stockItems=listOf(StockReportItem("u","كرتون جديد","كرتون",3.0,UnitType.CARTON,0.0,0.0,0)),totalProductsValue=150.0)}
        ui.onNodeWithText("كرتون جديد").assertExists();ui.onNodeWithText("وزن قديم").assertDoesNotExist()
        dark(false);ui.onNodeWithText("كرتون جديد").assertExists()
    }
    @Test fun expandedProductUnitRefreshesItsStockWhileKeepingItsMovementLedgerVisible(){
        val detail=mockk<ProductDetailViewModel>(relaxed=true)
        val unit=ProductUnit(id="u",productId="p",unitLabel="وحدة حية",quantityInStock=2.0,lowStockThreshold=3.0)
        val product=ProductWithUnits(Product(id="p",name="صنف حي"),listOf(unit))
        val live=MutableStateFlow(ProductDetailUiState(product=product,isLoading=false))
        every{detail.uiState} returns live
        every{detail.getMovementsForUnit("u")} returns flowOf(listOf(StockMovement(id="m",productId="p",unitId="u",quantity=2.0,quantityBefore=0.0,quantityAfter=2.0,note="مرجع الحركة")))
        showApp{ProductDetailScreen("p",{},{},detail)}
        ui.onNodeWithText(unit.unitLabel).performClick();dark(true)
        ui.runOnIdle{live.value=live.value.copy(product=product.copy(units=listOf(unit.copy(quantityInStock=10.0))))}
        ui.onNodeWithText("مرجع الحركة").performScrollTo().assertExists()
        dark(false);ui.onNodeWithText("خصم").performScrollTo().performClick()
        verify(exactly=1){detail.showAdjust(match{it.id=="u"&&it.quantityInStock==10.0},false)}
        verify(exactly=0){detail.applyAdjust()}
    }
}
