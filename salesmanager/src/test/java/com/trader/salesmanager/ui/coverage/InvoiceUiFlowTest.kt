package com.trader.salesmanager.ui.coverage

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.trader.core.domain.repository.ProductRepository
import com.trader.salesmanager.ui.inventory.invoice.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class InvoiceUiFlowTest:OfflineSellerUiTest() {
    private val repository=mockk<ProductRepository>(relaxed=true)
    private lateinit var vm:InvoiceItemsViewModel
    private val product=com.trader.core.domain.model.ProductWithUnits(
        com.trader.core.domain.model.Product(id="rice",name="أرز الاختبار",barcode="123",merchantId="fixture"),
        listOf(com.trader.core.domain.model.ProductUnit(id="piece",productId="rice",unitLabel="حبة",price=25.0,quantityInStock=4.0,isDefault=true)))
    @Before fun data() {
        every { repository.getAllProducts() } returns MutableStateFlow(listOf(product))
        vm=owned(InvoiceItemsViewModel(repository))
    }
    @Test fun emptyInvoiceShowsCustomerAndDisablesConfirmation() {
        var confirmations=0
        show { InvoiceItemsScreen("زبون الاختبار",{},onConfirm={_,_->confirmations++},viewModel=vm) }
        ui.onNodeWithText("زبون الاختبار",substring=true).assertExists()
        ui.onNodeWithText("أضف أصناف أولاً").assertIsNotEnabled()
        Assert.assertEquals(0,confirmations)
    }
    @Test fun confirmationReturnsActualLinesAndCalculatedTotal() {
        var result=0.0;var count=0
        show { InvoiceItemsScreen("زبون",{},onConfirm={lines,total->count=lines.size;result=total},viewModel=vm) }
        ui.runOnIdle { vm.addProduct(product);vm.updateDisplayQty(0,2.0) }
        ui.onNodeWithText("تأكيد  ₪50.00").performClick()
        Assert.assertEquals(1,count);Assert.assertEquals(50.0,result,0.0)
    }
    @Test fun editingArabicQuantityRecalculatesConfirmation() {
        show { InvoiceItemsScreen("زبون",{},onConfirm={_,_->},viewModel=vm) }
        ui.runOnIdle { vm.addProduct(product) }
        // The edit icon currently has no content description; it precedes the confirm action.
        ui.onAllNodes(hasClickAction()).let { actions -> actions[actions.fetchSemanticsNodes().size-2].performClick() }
        ui.onAllNodes(hasSetTextAction())[0].performTextReplacement("٣")
        ui.onNodeWithText("تطبيق").performClick()
        ui.onNodeWithText("تأكيد  ₪75.00").assertIsEnabled()
    }
    @Test fun zeroQuantityCannotBeApplied() {
        show { InvoiceItemsScreen("زبون",{},onConfirm={_,_->},viewModel=vm) }
        ui.runOnIdle { vm.addProduct(product) }
        // The edit icon currently has no content description; it precedes the confirm action.
        ui.onAllNodes(hasClickAction()).let { actions -> actions[actions.fetchSemanticsNodes().size-2].performClick() }
        ui.onAllNodes(hasSetTextAction())[0].performTextReplacement("0")
        ui.onNodeWithText("تطبيق").assertIsNotEnabled()
    }
}
