package com.trader.salesmanager.ui.qualification

import android.content.Context
import androidx.lifecycle.ViewModelStore
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.boxes.BoxesViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class CashFormContractsTest {
    private val cash=mockk<CashBoxRepository>(relaxed=true)
    private val methods=mockk<PaymentMethodRepository>()
    private val context=mockk<Context>(relaxed=true)
    private val store=ViewModelStore()
    @Before fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        every{context.applicationContext} returns context
        every{context.getString(any())} answers {"label:${firstArg<Int>()}"}
        every{cash.getAllBoxes()} returns flowOf(emptyList());every{cash.getAllMovements()} returns flowOf(emptyList())
        every{methods.getAllPaymentMethods()} returns flowOf(emptyList())
    }
    @After fun cleanup(){store.clear();Dispatchers.resetMain()}
    private fun model()=BoxesViewModel(cash,methods,context).also{store.put("cash",it)}
    private fun TestScope.assertRejected(text:String,initial:Boolean) {
        val vm=model();runCurrent()
        if(initial)vm.setInitialBalance("1",text) else vm.adjustBalance("1",text,"fixture",AdjustmentReason.COUNT)
        runCurrent();assertNotNull(vm.uiState.value.error)
        coVerify(exactly=0){cash.setInitialBalance(any(),any())};coVerify(exactly=0){cash.adjustBalance(any(),any(),any(),any())}
    }
    @Test fun nanInitialBalanceNeverReachesTheRepository()=runTest {assertRejected("NaN",true)}
    @Test fun infiniteInitialBalanceNeverReachesTheRepository()=runTest {assertRejected("Infinity",true)}
    @Test fun overflowingInitialBalanceNeverReachesTheRepository()=runTest {assertRejected("1e309",true)}
    @Test fun nanCorrectionNeverReachesTheRepository()=runTest {assertRejected("NaN",false)}
    @Test fun infiniteCorrectionNeverReachesTheRepository()=runTest {assertRejected("Infinity",false)}
    @Test fun overflowingCorrectionNeverReachesTheRepository()=runTest {assertRejected("1e309",false)}
    @Test fun cancelledInitializationDoesNotReportAnOrdinaryFailure()=runTest {
        coEvery{cash.setInitialBalance(any(),any())} throws CancellationException("caller fixture")
        val vm=model();runCurrent();vm.setInitialBalance("1","25");runCurrent();assertNull(vm.uiState.value.error)
    }
    @Test fun cancelledCorrectionDoesNotReportAnOrdinaryFailure()=runTest {
        coEvery{cash.adjustBalance(any(),any(),any(),any())} throws CancellationException("caller fixture")
        val vm=model();runCurrent();vm.adjustBalance("1","25",null,AdjustmentReason.CORRECTION);runCurrent();assertNull(vm.uiState.value.error)
    }
    @Test fun localizedDigitsDecimalValuesAndAllReasonLabelsArePreserved()=runTest {
        val vm=model();runCurrent();vm.setInitialBalance("1","١٢.٥٠");runCurrent()
        coVerify{cash.setInitialBalance("1",12.5)}
        for(reason in AdjustmentReason.entries){vm.adjustBalance("1","۲۵.۷۵","  fixture note  ",reason);runCurrent()
            coVerify{cash.adjustBalance("1",25.75,match{it?.endsWith(": fixture note")==true},reason)}}
        assertNull(vm.uiState.value.error)
    }
    @Test fun zeroRemainsValidAndInvalidBlankOrNegativeTextCannotCreateMutations()=runTest {
        val vm=model();runCurrent();vm.setInitialBalance("1","0");runCurrent();vm.adjustBalance("1","0",null,AdjustmentReason.OTHER);runCurrent()
        for(text in listOf("","bad","-1")){vm.setInitialBalance("1",text);vm.adjustBalance("1",text,null,AdjustmentReason.OTHER);runCurrent()}
        coVerify(exactly=1){cash.setInitialBalance("1",0.0)};coVerify(exactly=1){cash.adjustBalance("1",0.0,any(),AdjustmentReason.OTHER)}
        assertNotNull(vm.uiState.value.error);vm.clearError();assertNull(vm.uiState.value.error)
    }
}
