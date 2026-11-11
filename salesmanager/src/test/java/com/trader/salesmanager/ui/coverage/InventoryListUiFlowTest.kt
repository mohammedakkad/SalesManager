package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ProductRepository
import com.trader.core.util.NetworkMonitor
import com.trader.salesmanager.ui.inventory.list.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InventoryListUiFlowTest:OfflineSellerUiTest() {
    private fun product(id:String,name:String,qty:Double)=ProductWithUnits(Product(id=id,name=name,merchantId="fixture"),
        listOf(ProductUnit(id="u$id",productId=id,quantityInStock=qty,price=25.0,lowStockThreshold=2.0,isDefault=true)))
    private val repo=mockk<ProductRepository>(relaxed=true)
    private val network=mockk<NetworkMonitor>()
    @Test fun stockFiltersKeepLowAndOutOfStockProductsSeparate() {
        every { repo.getAllProducts() } returns MutableStateFlow(listOf(product("a","متوفر",10.0),product("b","قليل",1.0),product("c","منتهي",0.0)))
        every { network.isOnlineFlow } returns MutableStateFlow(false)
        val vm=owned(InventoryListViewModel(repo,network));var selected=""
        show { InventoryListScreen({}, {selected=it},{},{},viewModel=vm) }
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("قليل"))
        ui.onNodeWithText("قليل").assertExists()
        ui.onNode(hasText("نقص") and SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role,androidx.compose.ui.semantics.Role.Checkbox)).performClick()
        ui.onNodeWithText("قليل").assertExists();ui.onNodeWithText("متوفر").assertDoesNotExist()
        ui.onNode(hasText("نفد") and SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.Role,androidx.compose.ui.semantics.Role.Checkbox)).performClick()
        ui.runOnIdle { Assert.assertEquals(StockFilter.OUT,vm.uiState.value.filter) }
        ui.onNodeWithText("منتهي").performClick()
        Assert.assertEquals("c",selected)
    }
    @Test fun emptyStockOffersAnAddProductAction() {
        every { repo.getAllProducts() } returns MutableStateFlow(emptyList())
        every { network.isOnlineFlow } returns MutableStateFlow(true)
        val vm=owned(InventoryListViewModel(repo,network));var adds=0
        show { InventoryListScreen({}, {},{adds++},{},viewModel=vm) }
        ui.onNodeWithText("المخزن").assertExists()
        Assert.assertEquals(0,vm.uiState.value.totalProducts)
    }
}
