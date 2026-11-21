package com.trader.salesmanager.ui.inventory.detail

import androidx.lifecycle.ViewModelStore
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProductAdjustmentRecoveryTest {
    private val products=mockk<ProductRepository>(relaxed=true)
    private val stock=mockk<StockRepository>(relaxed=true)
    private val store=ViewModelStore()
    private val unit=ProductUnit(id="u",productId="p",unitLabel="كيس")
    @Before fun setup(){Dispatchers.setMain(StandardTestDispatcher());every{products.getAllProducts()} returns flowOf(listOf(ProductWithUnits(Product(id="p",name="صنف"),listOf(unit))))}
    @After fun cleanup(){store.clear();Dispatchers.resetMain()}
    private fun TestScope.model():ProductDetailViewModel=ProductDetailViewModel(products,stock,"p").also{vm->store.put("detail",vm);backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){vm.uiState.collect()};runCurrent();vm.showAdjust(unit,true);vm.setAdjustQty("2");vm.setAdjustNote("عد يدوي");runCurrent()}
    @Test fun repeatedSubmissionBeforeTheCoroutineStartsCannotAdjustTwice()=runTest {
        val gate=CompletableDeferred<Unit>();coEvery{stock.manualAdjust(any(),any(),any(),any(),any(),any(),any())} coAnswers{gate.await()}
        val vm=model();vm.applyAdjust();vm.applyAdjust();runCurrent()
        coVerify(exactly=1){stock.manualAdjust("p","u",2.0,MovementType.MANUAL_IN,"صنف","كيس","عد يدوي")}
        gate.complete(Unit);runCurrent();assertFalse(vm.uiState.value.isSaving);assertFalse(vm.uiState.value.adjustState.show)
    }
    @Test fun providerCancellationReleasesBusyStateAndPreservesTheEditableDraft()=runTest {
        coEvery{stock.manualAdjust(any(),any(),any(),any(),any(),any(),any())} throws CancellationException("provider cancelled")
        val vm=model();vm.applyAdjust();runCurrent()
        assertFalse("Cancellation left the stock dialog permanently busy",vm.uiState.value.isSaving)
        assertTrue(vm.uiState.value.adjustState.show);assertEquals("2",vm.uiState.value.adjustState.quantity);assertEquals("عد يدوي",vm.uiState.value.adjustState.note)
    }
    @Test fun ordinaryStorageFailureReleasesBusyStateAndKeepsTheDraft()=runTest {
        coEvery{stock.manualAdjust(any(),any(),any(),any(),any(),any(),any())} throws java.io.IOException("fixture ledger fault")
        val vm=model();vm.applyAdjust();runCurrent()
        assertFalse("Failed write left the dialog permanently busy",vm.uiState.value.isSaving)
        assertTrue(vm.uiState.value.adjustState.show);assertEquals("2",vm.uiState.value.adjustState.quantity)
    }
    @Test fun cancellationAllowsAnExplicitRetryWithTheNewQuantityAndNote()=runTest {
        coEvery{stock.manualAdjust(any(),any(),any(),any(),any(),any(),any())} throws CancellationException("provider cancelled") andThen Unit
        val vm=model();vm.applyAdjust();runCurrent();assertFalse(vm.uiState.value.isSaving)
        vm.setAdjustQty("3.5");vm.setAdjustNote("إعادة العد");vm.applyAdjust();runCurrent()
        coVerify(exactly=1){stock.manualAdjust("p","u",3.5,MovementType.MANUAL_IN,"صنف","كيس","إعادة العد")}
        assertFalse(vm.uiState.value.adjustState.show)
    }
}
