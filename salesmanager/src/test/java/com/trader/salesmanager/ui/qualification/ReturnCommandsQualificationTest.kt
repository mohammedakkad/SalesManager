package com.trader.salesmanager.ui.qualification

import androidx.lifecycle.ViewModelStore
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.returns.ReturnViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ReturnCommandsQualificationTest {
    private val returns=mockk<ReturnRepository>(relaxed=true);private val invoices=mockk<InvoiceItemRepository>(relaxed=true)
    private val products=mockk<ProductRepository>(relaxed=true);private val store=ViewModelStore()
    private val items=listOf(InvoiceItem("i",41,"p","أرز","u","كيس",3.0,20.0,60.0,"m1"),InvoiceItem("j",41,"p","أرز","v","علبة",2.0,10.0,20.0,"m1"))
    @Before fun setup(){
        Dispatchers.setMain(StandardTestDispatcher());coEvery{invoices.getItemsForTransactionOnce(41)} returns items;coEvery{returns.getAlreadyReturnedQty(any(),any())} returns 0.0
        coEvery{products.getProductById("p")} returns ProductWithUnits(Product(id="p",name="أرز"),listOf(ProductUnit("u","p",unitLabel="كيس",costPrice=12.0),ProductUnit("v","p",unitLabel="علبة",costPrice=6.0)))
        coEvery{returns.processReturn(any(),any())} coAnswers {firstArg()}
    }
    @After fun cleanup(){store.clear();Dispatchers.resetMain()}
    private fun model()=ReturnViewModel(returns,invoices,products,"m1",41).also{store.put("return",it)}
    @Test fun deselectedReturnableLineMakesReturnPartialAndKeepsItOutOfRefund()=runTest {
        val vm=model();runCurrent();vm.toggleLine(1)
        assertFalse("Unselected returnable items cannot be classified as a full return",vm.state.value.isFullReturn)
        assertEquals(60.0,vm.state.value.totalRefund,0.0);vm.confirmReturn();runCurrent()
        coVerify{returns.processReturn(match{it.returnType==ReturnType.PARTIAL&&it.originalTransactionId==41L},match{it.size==1&&it.single().unitId=="u"})}
    }
    @Test fun priorReturnsLimitQuantityAndRefundAndFullMeansAllRemainingItems()=runTest {
        coEvery{returns.getAlreadyReturnedQty(41,"u")} returns 1.0
        val vm=model();runCurrent();assertEquals(2.0,vm.state.value.lines[0].maxReturnable,0.0)
        vm.updateQty(0,99.0);assertEquals(2.0,vm.state.value.lines[0].returnQty,0.0)
        assertEquals(60.0,vm.state.value.totalRefund,0.0);assertEquals(24.0,vm.state.value.totalLostProfit,0.0)
        vm.updateNote("سبب");vm.showConfirmSheet();vm.confirmReturn();runCurrent()
        coVerify{returns.processReturn(match{it.returnType==ReturnType.FULL&&it.note=="سبب"&&it.totalRefund==60.0},match{it.sumOf{line->line.totalRefund}==60.0})}
        assertTrue(vm.state.value.processingState is ReturnUiState.Success);assertFalse(vm.state.value.showConfirmSheet)
    }
    @Test fun zeroQuantityCannotConfirmAndRepositoryFailureKeepsExplicitRetryState()=runTest {
        val vm=model();runCurrent();vm.updateQty(0,-1.0);vm.updateQty(1,0.0);assertFalse(vm.state.value.canConfirm)
        vm.confirmReturn();runCurrent();coVerify(exactly=0){returns.processReturn(any(),any())}
        vm.updateQty(0,1.0);coEvery{returns.processReturn(any(),any())} throws IllegalStateException("تجاوز الكمية")
        vm.confirmReturn();runCurrent();assertEquals(ReturnUiState.Error("تجاوز الكمية"),vm.state.value.processingState)
        vm.dismissConfirmSheet();assertEquals(ReturnUiState.Idle,vm.state.value.processingState);assertTrue(vm.state.value.canConfirm)
    }
}
