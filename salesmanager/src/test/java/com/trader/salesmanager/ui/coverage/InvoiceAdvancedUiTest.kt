package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ProductRepository
import com.trader.salesmanager.ui.inventory.invoice.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InvoiceAdvancedUiTest:OfflineSellerUiTest() {
    private val repo=mockk<ProductRepository>(relaxed=true)
    private val product=ProductWithUnits(Product(id="p",name="أرز موزون",merchantId="m1"),listOf(ProductUnit("u","p",unitType=UnitType.WEIGHT,unitLabel="كيلو",price=20.0,quantityInStock=10.0,isDefault=true),ProductUnit("v","p",unitLabel="حبة",price=5.0,quantityInStock=10.0)))
    private lateinit var vm:InvoiceItemsViewModel
    @Before fun fixture(){every{repo.getAllProducts()} returns MutableStateFlow(listOf(product));vm=owned(InvoiceItemsViewModel(repo))}
    @Test fun searchAddsChosenUnitAndCustomPriceIsIncludedInConfirmation() {
        var total=0.0
        show{InvoiceItemsScreen("زبون",{},onConfirm={_,amount->total=amount},viewModel=vm)}
        ui.onAllNodes(hasClickAction())[2].performClick();ui.onNode(hasSetTextAction()).performTextInput("أرز")
        ui.onNodeWithText("حبة").performClick()
        ui.onAllNodes(hasClickAction()).let{it[it.fetchSemanticsNodes().size-2].performClick()}
        ui.onAllNodes(hasSetTextAction())[0].performTextReplacement("2")
        ui.onAllNodes(hasSetTextAction())[1].performTextReplacement("7.5")
        ui.onNodeWithText("تطبيق").performClick();ui.onNodeWithText("تأكيد  ₪15.00").performClick();assertEquals(15.0,total,0.0)
    }
    @Test fun weightEditorConvertsGramsAndRemovingLineRestoresEmptyInvoice() {
        show{InvoiceItemsScreen("زبون",{},onConfirm={_,_->},viewModel=vm)}
        ui.runOnIdle{vm.addProduct(product)}
        ui.onAllNodes(hasClickAction()).let{it[it.fetchSemanticsNodes().size-2].performClick()}
        ui.onNodeWithText("غرام").performClick();ui.onAllNodes(hasSetTextAction())[0].performTextReplacement("500")
        ui.onNodeWithText("تطبيق").performClick();ui.onNodeWithText("تأكيد  ₪10.00").assertIsEnabled()
        ui.onAllNodes(hasClickAction()).let{it[it.fetchSemanticsNodes().size-2].performClick()};ui.onNodeWithText("حذف").performClick()
        ui.onNodeWithText("أضف أصناف أولاً").assertIsNotEnabled();ui.runOnIdle{assertTrue(vm.uiState.value.lines.isEmpty())}
    }
}
