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
import org.robolectric.annotation.Config

/** Draft warnings and prices are UI behavior; actual stock admission remains SaleCommands/Room. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers="w480dp-h900dp")
class InvoiceStockRefreshUiTest:AppThemeUiFixture(){
    private val unit=ProductUnit(id="u",productId="p",unitLabel="حبة مخزن",price=10.0,quantityInStock=1.0,isDefault=true)
    private val product=ProductWithUnits(Product(id="p",name="أرز مخزن"),listOf(unit))
    private val catalog=MutableStateFlow(listOf(product))
    private val repo=mockk<ProductRepository>(relaxed=true)
    private lateinit var vm:InvoiceItemsViewModel
    @Before fun fixture(){every{repo.getAllProducts()} returns catalog;vm=owned(InvoiceItemsViewModel(repo))}
    private fun ready(){ui.waitUntil(5000){ui.waitForIdle();!vm.uiState.value.isLoading}}
    private fun render(add:ProductWithUnits?=product,confirm:(List<InvoiceLineItem>,Double)->Unit={_,_->}){
        showApp{InvoiceItemsScreen("زبون مخزن",{},onConfirm=confirm,viewModel=vm)};ready()
        if(add!=null){ui.runOnIdle{vm.addProduct(add)};ui.waitUntil(5000){ui.waitForIdle();vm.uiState.value.lines.isNotEmpty()}}
    }
    @Test fun increasingPastAvailableStockShowsAWarningAndDecreasingClearsItWithoutAStockWrite(){
        var confirms=0;render(confirm={_,_->confirms++})
        ui.onNodeWithText("تأكيد  ₪10.00").assertExists();dark(true)
        ui.onNodeWithContentDescription("زيادة كمية أرز مخزن (حبة مخزن)").performClick()
        ui.onNodeWithText("بعض الأصناف تتجاوز الكمية المتاحة في المخزون").assertExists()
        ui.onNodeWithText("تأكيد رغم تجاوز المخزون  ₪20.00").assertExists();assertEquals(0,confirms)
        dark(false);ui.onNodeWithContentDescription("تقليل كمية أرز مخزن (حبة مخزن)").performClick()
        ui.onNodeWithText("بعض الأصناف تتجاوز الكمية المتاحة في المخزون").assertDoesNotExist()
        ui.onNodeWithText("تأكيد  ₪10.00").assertExists();assertEquals(0,confirms)
        coVerify(exactly=0){repo.saveProduct(any(),any(),any())}
    }
    @Test fun zeroAvailableStockExplainsTheDraftWarningAndThemeCannotConfirmTheInvoice(){
        val empty=product.copy(units=listOf(unit.copy(quantityInStock=0.0)))
        catalog.value=listOf(empty);var confirms=0;render(empty){_,_->confirms++}
        ui.onNodeWithText("بعض الأصناف تتجاوز الكمية المتاحة في المخزون").assertExists()
        dark(true);dark(false)
        ui.onNodeWithText("تأكيد رغم تجاوز المخزون  ₪10.00").assertIsEnabled()
        assertEquals(0,confirms);ui.runOnIdle{assertTrue(vm.uiState.value.lines.single().isOverStock)}
        coVerify(exactly=0){repo.saveProduct(any(),any(),any())}
    }
    @Test fun activeSearchReplacesCatalogResultsAndSelectionUsesTheNewUnitPrice(){
        var selected:List<InvoiceLineItem>?=null;var amount=0.0
        render(add=null){lines,total->selected=lines;amount=total}
        ui.onNodeWithContentDescription("البحث عن صنف").performClick()
        ui.onNode(hasSetTextAction()).performTextInput("أرز");ui.onNodeWithText("أرز مخزن").assertExists();dark(true)
        val next=ProductWithUnits(Product(id="q",name="أرز بديل"),listOf(unit.copy(id="v",productId="q",price=35.0,quantityInStock=5.0)))
        ui.runOnIdle{catalog.value=listOf(next)}
        ui.onNodeWithText("أرز مخزن").assertDoesNotExist();ui.onNodeWithText("أرز بديل").performClick()
        dark(false);ui.onNodeWithText("تأكيد  ₪35.00").performClick()
        assertEquals(35.0,amount,0.0);assertEquals("q",selected!!.single().product.product.id);assertEquals("v",selected!!.single().selectedUnit.id)
    }
    @Test fun explicitCustomPriceSurvivesUnrelatedCatalogReplacementAndThemeChanges(){
        var selected:List<InvoiceLineItem>?=null;var amount=0.0
        render(confirm={lines,total->selected=lines;amount=total})
        ui.runOnIdle{vm.updatePrice(0,7.5)}
        ui.onNodeWithText("معدّل").assertExists();dark(true)
        ui.runOnIdle{catalog.value=listOf(product.copy(units=listOf(unit.copy(price=50.0))))}
        dark(false);ui.onNodeWithText("تأكيد  ₪7.50").performClick()
        assertEquals(7.5,amount,0.0);assertEquals(7.5,selected!!.single().customPrice!!,0.0)
        coVerify(exactly=0){repo.saveProduct(any(),any(),any())}
    }
    @Test fun changingSelectedUnitResetsCustomPriceAndRecalculatesTheDraftWarning(){
        val next=unit.copy(id="v",unitLabel="كرتون مخزن",unitType=UnitType.CARTON,itemsPerCarton=12,price=20.0,quantityInStock=1.0,isDefault=false)
        val multi=product.copy(units=listOf(unit,next));catalog.value=listOf(multi)
        render(multi);ui.runOnIdle{vm.updatePrice(0,7.5);vm.updateDisplayQty(0,2.0)};dark(true)
        ui.runOnIdle{vm.updateUnit(0,next)}
        ui.onNodeWithText("معدّل").assertDoesNotExist();ui.onNodeWithText("تأكيد رغم تجاوز المخزون  ₪40.00").assertExists()
        ui.runOnIdle{assertNull(vm.uiState.value.lines.single().customPrice);assertEquals("v",vm.uiState.value.lines.single().selectedUnit.id)}
        dark(false);ui.runOnIdle{vm.updateDisplayQty(0,1.0)}
        ui.onNodeWithText("بعض الأصناف تتجاوز الكمية المتاحة في المخزون").assertDoesNotExist()
        ui.onNodeWithText("تأكيد  ₪20.00").assertExists()
    }
    @Test fun weightDisplayConversionPreservesKilogramsAndInvoiceTotalAcrossThemeChanges(){
        val weighted=product.copy(units=listOf(unit.copy(unitType=UnitType.WEIGHT,unitLabel="كيلو",quantityInStock=5.0,price=20.0)))
        catalog.value=listOf(weighted);render(weighted)
        ui.runOnIdle{vm.updateWeightUnit(0,SaleWeightUnit.GRAM)}
        ui.onNodeWithText("1000 غرام").assertExists();dark(true)
        ui.onNodeWithText("(1 كيلو)").assertExists();ui.onNodeWithText("تأكيد  ₪20.00").assertExists()
        dark(false);ui.runOnIdle{val line=vm.uiState.value.lines.single();assertEquals(1.0,line.quantity,0.0);assertEquals(20.0,line.totalPrice,0.0)}
        coVerify(exactly=0){repo.saveProduct(any(),any(),any())}
    }
}
