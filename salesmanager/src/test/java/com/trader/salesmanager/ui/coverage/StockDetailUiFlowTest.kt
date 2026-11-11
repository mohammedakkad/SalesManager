package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.inventory.detail.*
import com.trader.salesmanager.ui.inventory.reports.*
import io.mockk.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StockDetailUiFlowTest:OfflineSellerUiTest() {
    @Test fun manualStockDeductionUsesTheSelectedUnitAndNegativeQuantity() {
        val product=ProductWithUnits(Product(id="p",name="صنف التفاصيل",category="غذاء",merchantId="fixture"),listOf(ProductUnit(id="u",productId="p",unitLabel="حبة",price=25.0,quantityInStock=10.0,isDefault=true)))
        val repo=mockk<ProductRepository>(relaxed=true);val stock=mockk<StockRepository>(relaxed=true)
        every { repo.getAllProducts() } returns MutableStateFlow(listOf(product))
        every { stock.getMovementsForProduct(any(),any()) } returns flowOf(listOf(StockMovement(id="m",productId="p",unitId="u",quantity=-2.0,quantityBefore=12.0,quantityAfter=10.0)))
        val vm=owned(ProductDetailViewModel(repo,stock,"p"))
        show { ProductDetailScreen("p",{}, {},viewModel=vm) }
        ui.onNodeWithText("صنف التفاصيل").assertExists()
        ui.runOnIdle { vm.showAdjust(product.units.single(),false);vm.setAdjustQty("2");vm.setAdjustNote("جرد الاختبار") }
        ui.onNodeWithText("تطبيق").performClick()
        coVerify { stock.manualAdjust("p","u",-2.0,MovementType.MANUAL_OUT,"صنف التفاصيل","حبة","جرد الاختبار") }
    }
    @Test fun stockReportTabsShowCashDebtAndInventoryAlerts() {
        fun product(id:String,name:String,q:Double)=ProductWithUnits(Product(id=id,name=name),listOf(ProductUnit(id="u$id",productId=id,quantityInStock=q,price=25.0,lowStockThreshold=2.0)))
        val vm=mockk<StockReportsViewModel>(relaxed=true)
        every { vm.uiState } returns MutableStateFlow(StockReportsUiState(
            stockItems=listOf(StockReportItem("u","أرز تقرير","حبة",10.0,UnitType.PIECE,3.0,75.0,2)),
            lowStockItems=listOf(product("a","قليل تقرير",1.0)),outOfStockItems=listOf(product("b","منتهي تقرير",0.0)),
            cashSummary=CashSummary(100.0,50.0,25.0,75.0,100.0),totalProductsValue=250.0,isLoading=false))
        show { StockReportsScreen({},viewModel=vm) }
        ui.onNodeWithText("قيمة المخزن الإجمالية").assertExists()
        ui.onNodeWithText("الكاش").performClick()
        ui.onNodeWithText("إجمالي الكاش المحصّل").assertExists()
        ui.onNodeWithText("تنبيهات").performClick()
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("منتهي تقرير"))
        ui.onNodeWithText("منتهي تقرير").assertExists()
    }
}
