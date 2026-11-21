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

/** Quantity controls must expose their action and the exact line to accessibility users. */
@RunWith(RobolectricTestRunner::class)
class InvoiceQuantityAccessibilityUiTest:OfflineSellerUiTest(){
    private val repo=mockk<ProductRepository>(relaxed=true)
    private fun product(id:String,name:String)=ProductWithUnits(Product(id=id,name=name),listOf(ProductUnit(id="u$id",productId=id,unitLabel="حبة",price=10.0,quantityInStock=10.0,isDefault=true)))
    private val first=product("a","قهوة مختارة")
    private val second=product("b","شاي باق")
    private lateinit var vm:InvoiceItemsViewModel
    @Before fun fixture(){every{repo.getAllProducts()} returns MutableStateFlow(listOf(first,second));vm=owned(InvoiceItemsViewModel(repo))}
    private fun render(back:()->Unit={},confirm:(List<InvoiceLineItem>,Double)->Unit={_,_->}){
        show{InvoiceItemsScreen("زبون الوصول",back,onConfirm=confirm,viewModel=vm)}
        ui.runOnIdle{vm.addProduct(first);vm.addProduct(second)}
        ui.waitUntil(5000){ui.waitForIdle();vm.uiState.value.lines.size==2}
    }
    @Test fun accessibleIncreaseNamesItsLineAndOnlyIncrementsThatLine(){
        render()
        ui.onNodeWithContentDescription("زيادة كمية قهوة مختارة (حبة)").performClick()
        ui.runOnIdle{assertEquals(listOf(2.0,1.0),vm.uiState.value.lines.map{it.displayQty});assertEquals(30.0,vm.uiState.value.totalAmount,0.0)}
        ui.onNodeWithText("تأكيد  ₪30.00").assertIsEnabled()
        coVerify(exactly=0){repo.saveProduct(any(),any(),any())}
    }
    @Test fun accessibleDecreaseRemovesTheSelectedLastUnitAndKeepsTheOtherInvoiceLine(){
        render()
        ui.onNodeWithContentDescription("تقليل كمية قهوة مختارة (حبة)").performClick()
        ui.waitUntil(5000){vm.uiState.value.lines.size==1}
        ui.runOnIdle{assertEquals("b",vm.uiState.value.lines.single().product.product.id);assertEquals(10.0,vm.uiState.value.totalAmount,0.0)}
        ui.onNodeWithContentDescription("زيادة كمية قهوة مختارة (حبة)").assertDoesNotExist()
        ui.onNodeWithContentDescription("زيادة كمية شاي باق (حبة)").assertExists()
    }
    @Test fun accessibleEditOpensTheNamedLineAndConfirmationUsesItsEditedQuantityAndPrice(){
        var confirmed:List<InvoiceLineItem>?=null;var total=0.0
        render{lines,amount->confirmed=lines;total=amount}
        ui.onNodeWithContentDescription("تعديل قهوة مختارة (حبة)").performClick()
        val fields=ui.onAllNodes(hasSetTextAction() and hasAnyAncestor(isDialog()))
        fields.assertCountEquals(2);fields[0].performTextReplacement("2");fields[1].performTextReplacement("7.5")
        ui.onNodeWithText("تطبيق").performClick()
        ui.onNodeWithText("تأكيد  ₪25.00").performClick()
        assertEquals(25.0,total,0.0)
        assertEquals(listOf("a","b"),confirmed!!.map{it.product.product.id})
        assertEquals(listOf(2.0,1.0),confirmed!!.map{it.quantity})
        assertEquals(7.5,confirmed!![0].effectivePrice,0.0);assertEquals(10.0,confirmed!![1].effectivePrice,0.0)
        coVerify(exactly=0){repo.saveProduct(any(),any(),any())}
    }
    @Test fun accessibleBackLeavesTheInvoiceWithoutConfirmingItsDraft(){
        var backs=0;var confirms=0
        render(back={backs++}){_,_->confirms++}
        ui.onNodeWithContentDescription("رجوع").performClick()
        assertEquals(1,backs);assertEquals(0,confirms)
        ui.runOnIdle{assertEquals(2,vm.uiState.value.lines.size)}
        coVerify(exactly=0){repo.saveProduct(any(),any(),any())}
    }
    @Test fun accessibleSearchSelectsTheNamedProductWithoutChangingTheOtherLine(){
        render()
        ui.onNodeWithContentDescription("البحث عن صنف").performClick()
        ui.onNode(hasSetTextAction()).performTextInput("شاي")
        ui.onNode(hasText("شاي باق") and hasClickAction()).performClick()
        ui.runOnIdle{assertEquals(listOf(1.0,2.0),vm.uiState.value.lines.map{it.displayQty})}
        ui.onNodeWithContentDescription("مسح الباركود").assertIsEnabled()
        ui.onNodeWithText("تأكيد  ₪30.00").assertIsEnabled()
    }

}
