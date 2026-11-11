package com.trader.salesmanager.ui.qualification

import android.app.Application
import androidx.lifecycle.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ProductRepository
import com.trader.salesmanager.ui.inventory.invoice.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class InvoiceDraftLifecycleTest {
    private val repo=mockk<ProductRepository>()
    private val kg=ProductUnit("kg","p",UnitType.WEIGHT,"كيلو",price=20.0,quantityInStock=8.0,isDefault=true)
    private val piece=ProductUnit("piece","p",UnitType.PIECE,"حبة",price=5.0,quantityInStock=3.0)
    private val product=ProductWithUnits(Product("p",name="أرز",barcode="7788",category="غذاء"),listOf(kg,piece))
    private val products=MutableStateFlow(listOf(product))
    private lateinit var vm:InvoiceItemsViewModel
    @Before fun setup(){Dispatchers.setMain(StandardTestDispatcher());every{repo.getAllProducts()} returns products;coEvery{repo.getProductById("p")} returns product;vm=InvoiceItemsViewModel(repo)}
    @After fun cleanup(){vm.viewModelScope.cancel();Dispatchers.resetMain()}
    private fun TestScope.collect(){backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){vm.uiState.collect{}};runCurrent()}
    private fun json(unit:String="kg",qty:Double=2.0,price:Double=20.0,weight:String="KG")=JSONArray().put(JSONObject().put("productId","p").put("unitId",unit).put("price",price).put("displayQty",qty).put("displayWeightUnit",weight)).toString()
    @Test fun duplicateDefaultProductIncrementsOneLineAndClearsSearch()=runTest {
        collect();vm.setQuery("أرز");vm.addProduct(product);vm.addProduct(product);runCurrent()
        assertEquals("",vm.uiState.value.searchQuery);val line=vm.uiState.value.lines.single();assertEquals(2.0,line.displayQty,0.0);assertEquals(40.0,vm.uiState.value.totalAmount,0.0)
    }
    @Test fun productWithoutUnitsCannotInventAnInvoiceLine()=runTest {collect();vm.addProduct(product.copy(units=emptyList()));runCurrent();assertTrue(vm.uiState.value.lines.isEmpty())}
    @Test fun explicitUnitsRemainSeparateAndDuplicateChosenUnitIncrementsOnlyThatLine()=runTest {
        collect();vm.addProduct(product);vm.addProductWithUnit(product,piece);vm.addProductWithUnit(product,piece);runCurrent()
        assertEquals(2,vm.uiState.value.lines.size);assertEquals(listOf(1.0,2.0),vm.uiState.value.lines.map{it.displayQty});assertEquals(30.0,vm.uiState.value.totalAmount,0.0)
    }
    @Test fun unitChangeUsesItsOwnStockAndResetsCustomPrice()=runTest {
        collect();vm.addProduct(product);vm.updatePrice(0,12.0);vm.updateUnit(0,piece);runCurrent()
        val line=vm.uiState.value.lines.single();assertEquals("piece",line.selectedUnit.id);assertNull(line.customPrice);assertEquals(3.0,line.stockAvailable,0.0);assertEquals(5.0,line.totalPrice,0.0)
    }
    @Test fun changingWeightDisplayPreservesStoredKilogramsAndTheLocalCommercialPoundRule()=runTest {
        collect();vm.addProduct(product);vm.updateDisplayQty(0,2.0)
        for(w in SaleWeightUnit.entries){vm.updateWeightUnit(0,w);runCurrent();assertEquals(2.0,vm.uiState.value.lines.single().quantity,0.0000001)}
        assertEquals(0.5,vm.uiState.value.lines.single().displayQty,0.0);assertEquals(4.0,SaleWeightUnit.POUND.toKg,0.0)
    }
    @Test fun zeroAndNegativeQuantitiesRemoveTheChosenLineWithoutChangingOthers()=runTest {
        collect();vm.addProduct(product);vm.addProductWithUnit(product,piece);vm.updateQuantity(0,0.0);runCurrent();assertEquals("piece",vm.uiState.value.lines.single().selectedUnit.id)
        vm.updateDisplayQty(0,-1.0);runCurrent();assertTrue(vm.uiState.value.lines.isEmpty())
    }
    @Test fun nullCustomPriceRestoresCatalogPriceAndZeroCustomPriceRemainsValid()=runTest {
        collect();vm.addProduct(product);vm.updatePrice(0,0.0);runCurrent();assertEquals(0.0,vm.uiState.value.totalAmount,0.0)
        vm.updatePrice(0,null);runCurrent();assertEquals(20.0,vm.uiState.value.totalAmount,0.0)
    }
    @Test fun stockWarningComparesConvertedKgAndAllowsTheExactBoundary()=runTest {
        collect();vm.addProduct(product);vm.updateWeightUnit(0,SaleWeightUnit.POUND);vm.updateDisplayQty(0,2.0);runCurrent();assertFalse(vm.uiState.value.hasOverStockWarnings)
        vm.updateDisplayQty(0,2.5);runCurrent();val line=vm.uiState.value.lines.single();assertTrue(line.isOverStock);assertEquals(2.0,line.overStockAmount,0.0);assertTrue(vm.uiState.value.hasOverStockWarnings)
    }
    @Test fun projectionUsesConvertedQuantityPriceMerchantAndTransactionIdentity()=runTest {
        collect();vm.addProduct(product);vm.updateWeightUnit(0,SaleWeightUnit.GRAM);vm.updateDisplayQty(0,500.0);vm.updatePrice(0,12.0);runCurrent()
        val a=vm.toInvoiceItems(42,"merchant").single();assertEquals(42L,a.transactionId);assertEquals("merchant",a.merchantId);assertEquals("p",a.productId);assertEquals("kg",a.unitId);assertEquals("أرز",a.productName)
        assertEquals("كيلو (غرام)",a.unitLabel);assertEquals(.5,a.quantity,0.0);assertEquals(12.0,a.pricePerUnit,0.0);assertEquals(6.0,a.totalPrice,0.0);assertTrue(a.id.isNotBlank())
    }
    @Test fun pieceProjectionDoesNotApplyAWeightConversionOrLabel()=runTest {
        collect();vm.addProductWithUnit(product,piece);vm.updateQuantity(0,2.0);runCurrent();val a=vm.toInvoiceItems(7,"m").single();assertEquals("حبة",a.unitLabel);assertEquals(2.0,a.quantity,0.0);assertEquals(10.0,a.totalPrice,0.0)
    }
    @Test fun validPersistedWeightDraftRestoresPriceStockAndKilograms()=runTest {
        collect();vm.loadExistingLines(json(qty=500.0,price=12.0,weight="GRAM"));runCurrent();val line=vm.uiState.value.lines.single()
        assertEquals(.5,line.quantity,0.0);assertEquals(12.0,line.customPrice!!,0.0);assertEquals(8.0,line.stockAvailable,0.0);assertEquals(6.0,vm.uiState.value.totalAmount,0.0)
    }
    @Test fun matchingCatalogPriceRestoresWithoutRedundantOverride()=runTest {collect();vm.loadExistingLines(json());runCurrent();assertNull(vm.uiState.value.lines.single().customPrice)}
    @Test fun legacyDraftWithoutWeightKeyDefaultsToKilograms()=runTest {
        collect();val obj=JSONArray(json()).getJSONObject(0).apply{remove("displayWeightUnit")};vm.loadExistingLines(JSONArray().put(obj).toString());runCurrent();assertEquals(SaleWeightUnit.KG,vm.uiState.value.lines.single().displayWeightUnit)
    }
    @Test fun unknownLegacyWeightLabelUsesTheExistingKilogramFallback()=runTest {collect();vm.loadExistingLines(json(weight="unknown"));runCurrent();assertEquals(SaleWeightUnit.KG,vm.uiState.value.lines.single().displayWeightUnit)}
    @Test fun missingCatalogRecordsCannotCreateInventedInvoiceItems()=runTest {
        collect();coEvery{repo.getProductById("missing")} returns null
        vm.loadExistingLines(json().replace("\"productId\":\"p\"","\"productId\":\"missing\""));runCurrent();assertTrue(vm.uiState.value.lines.isEmpty())
        vm.loadExistingLines(json(unit="missing"));runCurrent();assertTrue(vm.uiState.value.lines.isEmpty())
    }
    @Test fun malformedDraftPreservesThePreviousBasketInsteadOfPartiallyReplacingIt()=runTest {
        collect();vm.addProductWithUnit(product,piece);runCurrent();val previous=vm.uiState.value.lines
        vm.loadExistingLines("["+JSONArray(json()).getJSONObject(0)+",{}]");runCurrent();assertEquals(previous,vm.uiState.value.lines)
        vm.loadExistingLines("broken");runCurrent();assertEquals(previous,vm.uiState.value.lines)
    }
    @Test fun catalogueLookupCancellationCancelsTheLoadJobAndPreservesTheDraft()=runTest {
        collect();vm.addProductWithUnit(product,piece);runCurrent();val previous=vm.uiState.value.lines
        coEvery{repo.getProductById("p")} throws CancellationException("offline cancel")
        val parent=vm.viewModelScope.coroutineContext[Job]!!;val before=parent.children.toSet();vm.loadExistingLines(json());val job=(parent.children.toSet()-before).single();runCurrent()
        assertTrue("Cancellation was swallowed by draft restoration",job.isCancelled);assertEquals(previous,vm.uiState.value.lines)
    }
    @Test fun matchingBarcodeAddsTheRealProductAndUnknownBarcodeReturnsItsExactText()=runTest {
        collect();coEvery{repo.getProductByBarcode("7788")} returns product;coEvery{repo.getProductByBarcode("missing+12")} returns null
        var missing:String?=null;vm.onBarcodeScanned("7788"){missing=it};runCurrent();assertNull(missing);assertEquals("p",vm.uiState.value.lines.single().product.product.id)
        vm.onBarcodeScanned("missing+12"){missing=it};runCurrent();assertEquals("missing+12",missing);assertEquals(1,vm.uiState.value.lines.size)
    }
    @Test fun searchIncludesNameBarcodeAndCategoryAndCapsVisibleResultsAtTen()=runTest {
        collect();products.value=(0..11).map{product.copy(product=product.product.copy(id="p$it"))};runCurrent()
        for(q in listOf("أرز","7788","غذاء")){vm.setQuery(q);runCurrent();assertEquals(10,vm.uiState.value.searchResults.size)}
        vm.clearQuery();runCurrent();assertTrue(vm.uiState.value.searchResults.isEmpty())
    }
    @Test fun clearLinesRemovesTotalsAndStockWarningsTogether()=runTest {
        collect();vm.addProduct(product);vm.updateDisplayQty(0,100.0);vm.clearLines();runCurrent();assertEquals(0.0,vm.uiState.value.totalAmount,0.0);assertFalse(vm.uiState.value.hasOverStockWarnings);assertTrue(vm.toInvoiceItems(1,"m").isEmpty())
    }
    @Test fun weightLabelsDistinguishCommercialDisplayFromKilograms(){
        val a=InvoiceLineItem(product=product,selectedUnit=kg,displayQty=2.0,displayWeightUnit=SaleWeightUnit.OZ)
        assertEquals("2 أوقية",a.displayQtyLabel);assertEquals("(0.5 كيلو)",a.kgLabel)
        val b=a.copy(selectedUnit=piece);assertEquals("2",b.displayQtyLabel);assertEquals("",b.kgLabel)
    }
}
