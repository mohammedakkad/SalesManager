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
    @Test fun duplicateConfirmationIsClaimedBeforeCoroutineDispatch()=runTest {
        val vm=model();runCurrent();vm.updateQty(0,1.0);vm.toggleLine(1)
        vm.confirmReturn();vm.confirmReturn();runCurrent()
        coVerify(exactly=1){returns.processReturn(any(),any())}
        assertTrue(vm.state.value.processingState is ReturnUiState.Success)
    }
    @Test fun dismissingPendingConfirmationCannotOpenAnotherSubmission()=runTest {
        val gate=CompletableDeferred<Unit>()
        coEvery{returns.processReturn(any(),any())} coAnswers {gate.await();firstArg()}
        val vm=model();runCurrent();vm.confirmReturn();runCurrent()
        vm.dismissConfirmSheet();vm.confirmReturn();runCurrent()
        coVerify(exactly=1){returns.processReturn(any(),any())}
        assertEquals(ReturnUiState.Loading,vm.state.value.processingState)
        gate.complete(Unit);runCurrent()
    }
    @Test fun pendingSubmissionFreezesItsQuantitySelectionAndNote()=runTest {
        val gate=CompletableDeferred<Unit>()
        coEvery{returns.processReturn(any(),any())} coAnswers {gate.await();firstArg()}
        val vm=model();runCurrent();vm.updateNote("original");vm.confirmReturn();runCurrent()
        val before=vm.state.value
        vm.updateQty(0,1.0);vm.toggleLine(1);vm.updateNote("changed")
        assertEquals(before.lines,vm.state.value.lines);assertEquals("original",vm.state.value.note)
        gate.complete(Unit);runCurrent()
    }
    @Test fun cancellationDoesNotBecomeAFinancialFailureMessage()=runTest {
        coEvery{returns.processReturn(any(),any())} throws CancellationException("fixture cancellation")
        val vm=model();runCurrent();vm.confirmReturn();runCurrent()
        assertEquals(ReturnUiState.Idle,vm.state.value.processingState)
    }
    @Test fun unchangedRetryKeepsInvoiceAndItemIdentitiesAfterUncertainFailure()=runTest {
        val seen=mutableListOf<Pair<ReturnInvoice,List<ReturnItem>>>()
        coEvery{returns.processReturn(any(),any())} coAnswers {
            val request=firstArg<ReturnInvoice>() to secondArg<List<ReturnItem>>();seen+=request
            if(seen.size==1) throw java.io.IOException("uncertain completion")
            request.first
        }
        val vm=model();runCurrent();vm.confirmReturn();runCurrent();vm.dismissConfirmSheet();vm.confirmReturn();runCurrent()
        assertEquals(2,seen.size);assertEquals("Retry must not create another refund identity",seen[0],seen[1])
    }
    @Test fun failedReturnedQuantityReadNeverInventsZeroReturnedStock()=runTest {
        coEvery{returns.getAlreadyReturnedQty(41,"u")} throws java.io.IOException("offline fixture")
        val vm=model();runCurrent()
        assertFalse("Unknown prior returns must not enable a full refund",vm.state.value.canConfirm)
        assertFalse(vm.state.value.isLoading);assertTrue(vm.state.value.processingState is ReturnUiState.Error)
        assertTrue(vm.state.value.lines.isEmpty())
    }
    @Test fun cancelledReturnedQuantityReadCannotCreateReturnableLines()=runTest {
        coEvery{returns.getAlreadyReturnedQty(41,"u")} throws CancellationException("cancelled read")
        val vm=model();runCurrent()
        assertTrue(vm.state.value.lines.isEmpty());assertFalse(vm.state.value.isLoading)
        assertFalse(vm.state.value.processingState is ReturnUiState.Error)
    }
    private fun TestScope.assertNonfiniteQuantityIgnored(value:Double) {
        val vm=model();runCurrent();vm.updateQty(0,1.0);vm.updateQty(0,value)
        assertEquals(1.0,vm.state.value.lines[0].returnQty,0.0)
        assertTrue(vm.state.value.canConfirm)
    }
    @Test fun nanQuantityCannotCorruptAValidSelection()=runTest {assertNonfiniteQuantityIgnored(Double.NaN)}
    @Test fun positiveInfiniteQuantityCannotBecomeAFullRefund()=runTest {assertNonfiniteQuantityIgnored(Double.POSITIVE_INFINITY)}
    @Test fun negativeInfiniteQuantityCannotEraseAValidSelection()=runTest {assertNonfiniteQuantityIgnored(Double.NEGATIVE_INFINITY)}
    @Test fun overflowingRefundCannotEnableConfirmation()=runTest {
        coEvery{invoices.getItemsForTransactionOnce(41)} returns listOf(items[0].copy(pricePerUnit=Double.MAX_VALUE))
        val vm=model();runCurrent();assertFalse(vm.state.value.canConfirm)
    }
    @Test fun overflowingSumOfFiniteRefundLinesCannotEnableConfirmation()=runTest {
        coEvery{invoices.getItemsForTransactionOnce(41)} returns items.map{it.copy(quantity=1.0,pricePerUnit=1e308)}
        val vm=model();runCurrent();assertFalse(vm.state.value.canConfirm)
    }
    @Test fun nonfiniteCostDoesNotEnterProfitOrReturnPayload()=runTest {
        coEvery{products.getProductById("p")} returns ProductWithUnits(Product("p"),listOf(ProductUnit("u","p",costPrice=Double.NaN)))
        val vm=model();runCurrent();assertFalse(vm.state.value.canConfirm)
    }
    @Test fun invalidOriginalQuantityDoesNotBecomeAReturnDraft()=runTest {
        coEvery{invoices.getItemsForTransactionOnce(41)} returns listOf(items[0].copy(quantity=Double.POSITIVE_INFINITY))
        val vm=model();runCurrent();assertFalse(vm.state.value.canConfirm)
    }
    @Test fun negativePriorReturnedQuantityDoesNotInflateAvailableRefund()=runTest {
        coEvery{returns.getAlreadyReturnedQty(41,"u")} returns -1.0
        val vm=model();runCurrent();assertFalse(vm.state.value.canConfirm)
    }
    @Test fun missingMerchantCannotSubmitARefund()=runTest {
        val vm=ReturnViewModel(returns,invoices,products,"",41).also{store.put("return",it)}
        runCurrent();vm.confirmReturn();runCurrent();coVerify(exactly=0){returns.processReturn(any(),any())}
    }

    @Test fun completedReturnCannotBeSubmittedAgainByDismissingItsSheet()=runTest {
        val vm=model();runCurrent();vm.confirmReturn();runCurrent()
        vm.dismissConfirmSheet();vm.confirmReturn();runCurrent()
        coVerify(exactly=1){returns.processReturn(any(),any())}
        assertTrue(vm.state.value.processingState is ReturnUiState.Success)
    }
    @Test fun unchangedQuantityCallbackDoesNotDiscardTheRetryIdentity()=runTest {
        val seen=mutableListOf<ReturnInvoice>()
        coEvery{returns.processReturn(any(),any())} coAnswers {
            val request=firstArg<ReturnInvoice>();seen+=request
            if(seen.size==1)throw java.io.IOException("uncertain completion");request
        }
        val vm=model();runCurrent();vm.confirmReturn();runCurrent()
        vm.updateQty(0,vm.state.value.lines[0].returnQty)
        vm.dismissConfirmSheet();vm.confirmReturn();runCurrent()
        assertEquals(2,seen.size);assertEquals(seen[0],seen[1])
    }

    @Test fun failedLoadHasAnExplicitRetryAndOnlyOneReloadCanBePending()=runTest {
        val gate=CompletableDeferred<Unit>();var attempts=0
        coEvery{returns.getAlreadyReturnedQty(41,"u")} coAnswers {
            attempts++;if(attempts==1)throw java.io.IOException("read fixture");gate.await();1.0
        }
        val vm=model();runCurrent();assertNotNull(vm.state.value.loadError)
        vm.retryLoad();vm.retryLoad();runCurrent()
        assertEquals(2,attempts);assertTrue(vm.state.value.isLoading);assertFalse(vm.state.value.canConfirm)
        gate.complete(Unit);runCurrent()
        assertNull(vm.state.value.loadError);assertTrue(vm.state.value.canConfirm)
        assertEquals(2.0,vm.state.value.lines[0].maxReturnable,0.0)
    }
    @Test fun missingProductKeepsTheExistingZeroCostFallbackAndAllowsZeroPriceReturns()=runTest {
        coEvery{invoices.getItemsForTransactionOnce(41)} returns listOf(items[0].copy(pricePerUnit=0.0,totalPrice=0.0))
        coEvery{products.getProductById("p")} returns null
        val vm=model();runCurrent();assertTrue(vm.state.value.canConfirm);vm.confirmReturn();runCurrent()
        coVerify{returns.processReturn(match{it.totalRefund==0.0},match{it.single().costPricePerUnit==0.0&&it.single().totalRefund==0.0})}
    }
    @Test fun invoiceReadFailureEndsLoadingAndCanBeRetriedWithoutAnOldDraft()=runTest {
        coEvery{invoices.getItemsForTransactionOnce(41)} throws java.io.IOException("read fixture")
        val vm=model();runCurrent();assertFalse(vm.state.value.isLoading);assertFalse(vm.state.value.canConfirm)
        assertTrue(vm.state.value.lines.isEmpty());assertNotNull(vm.state.value.loadError)
        coEvery{invoices.getItemsForTransactionOnce(41)} returns items
        vm.retryLoad();runCurrent();assertTrue(vm.state.value.canConfirm);assertNull(vm.state.value.loadError)
    }

}
