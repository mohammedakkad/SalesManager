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
class ProductDetailViewModelTest {
    private val products=mockk<ProductRepository>(relaxed=true)
    private val stock=mockk<StockRepository>(relaxed=true)
    private val store=ViewModelStore()
    private val unit=ProductUnit(id="u",productId="p",unitLabel="كيس")
    @Before fun setup(){Dispatchers.setMain(StandardTestDispatcher());every{products.getAllProducts()} returns flowOf(listOf(ProductWithUnits(Product(id="p",name="صنف"),listOf(unit))))}
    @After fun cleanup(){store.clear();Dispatchers.resetMain()}
    private fun model()=ProductDetailViewModel(products,stock,"p").also{store.put("detail",it)}
    @Test fun invalidAndNonFiniteQuantitiesNeverReachStockRepository()=runTest{
        val vm=model();vm.showAdjust(unit,true)
        for(value in listOf("","bad","0","-1","NaN","Infinity","-Infinity")){
            vm.setAdjustQty(value);vm.applyAdjust();runCurrent()
        }
        coVerify(exactly=0){stock.manualAdjust(any(),any(),any(),any(),any(),any(),any())}
    }
    @Test fun additionPreservesDecimalQuantityAndNoteThenClearsTheDialog()=runTest{
        val vm=model();backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){vm.uiState.collect()};runCurrent()
        vm.showAdjust(unit,true);vm.setAdjustQty("1.25");vm.setAdjustNote("تسوية العد");vm.applyAdjust();runCurrent()
        coVerify(exactly=1){stock.manualAdjust("p","u",1.25,MovementType.MANUAL_IN,"صنف","كيس","تسوية العد")}
        assertFalse(vm.uiState.value.isSaving);assertFalse(vm.uiState.value.adjustState.show)
    }
    @Test fun deductionWaitsForRepositoryCompletionBeforeDismissingDialog()=runTest{
        val gate=CompletableDeferred<Unit>();coEvery{stock.manualAdjust(any(),any(),any(),any(),any(),any(),any())} coAnswers{gate.await()}
        val vm=model();backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){vm.uiState.collect()};runCurrent()
        vm.showAdjust(unit,false);vm.setAdjustQty("2");vm.applyAdjust();runCurrent()
        assertTrue(vm.uiState.value.isSaving);assertTrue(vm.uiState.value.adjustState.show)
        coVerify(exactly=1){stock.manualAdjust("p","u",-2.0,MovementType.MANUAL_OUT,"صنف","كيس","")}
        gate.complete(Unit);runCurrent();assertFalse(vm.uiState.value.isSaving);assertFalse(vm.uiState.value.adjustState.show)
    }
    @Test fun deletingNeverAcknowledgesBeforeRepositoryCompletes()=runTest{
        val gate=CompletableDeferred<Unit>();coEvery{products.deleteProduct("p")} coAnswers{gate.await()}
        val vm=model();var deleted=0;vm.deleteProduct{deleted++};runCurrent();assertEquals(0,deleted)
        gate.complete(Unit);runCurrent();assertEquals(1,deleted);coVerify(exactly=1){products.deleteProduct("p")}
    }
}
