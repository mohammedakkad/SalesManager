package com.trader.salesmanager.sales

import androidx.lifecycle.viewModelScope
import com.trader.core.domain.repository.ProductRepository
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.inventory.addedit.AddEditProductViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalCoroutinesApi::class)
class ProductBarcodeContractsTest {
    private val repository=mockk<ProductRepository>()
    private lateinit var vm:AddEditProductViewModel
    @Before fun setup(){Dispatchers.setMain(StandardTestDispatcher());vm=AddEditProductViewModel(repository)}
    @After fun close(){vm.viewModelScope.cancel();Dispatchers.resetMain()}
    @Test fun barcodeCheckingIsClaimedBeforeDebounceSoDraftCannotSubmitPrematurely()=runTest {
        coEvery { repository.getBarcodeConflict(any(),any()) } returns null
        vm.setName("fixture");vm.updateUnitPrice(0,"12")
        vm.setBarcode("123")
        assertTrue(vm.uiState.value.barcodeChecking)
        assertFalse(vm.uiState.value.isValid)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.barcodeChecking)
        assertTrue(vm.uiState.value.isValid)
    }
    @Test fun nonCooperativeOldReplyCannotOverwriteNewBarcodeResult()=runTest {
        lateinit var old:Continuation<String?>
        coEvery { repository.getBarcodeConflict("old",null) } coAnswers {suspendCoroutine {old=it}}
        coEvery { repository.getBarcodeConflict("new",null) } returns null
        vm.setBarcode("old");advanceTimeBy(350);runCurrent()
        vm.setBarcode("new");advanceTimeBy(350);runCurrent()
        assertEquals("new",vm.uiState.value.barcode)
        assertNull(vm.uiState.value.barcodeConflict)
        old.resume("obsolete conflict");runCurrent()
        assertEquals("new",vm.uiState.value.barcode)
        assertNull(vm.uiState.value.barcodeConflict)
        assertFalse(vm.uiState.value.barcodeChecking)
    }
    @Test fun queryFailureEndsCheckingAndSurfacesAnExplicitRetryableError()=runTest {
        coEvery { repository.getBarcodeConflict("123",null) } throws java.io.IOException("offline fixture")
        vm.setBarcode("123");advanceUntilIdle()
        assertFalse(vm.uiState.value.barcodeChecking)
        assertNotNull(vm.uiState.value.error)
        assertEquals("123",vm.uiState.value.barcode)
    }

    @Test fun loadingExistingProductInvalidatesAnEarlierCheckOfTheSameBarcode()=runTest {
        lateinit var old:Continuation<String?>
        coEvery { repository.getBarcodeConflict("same",null) } coAnswers {suspendCoroutine {old=it}}
        coEvery { repository.getProductById("persisted") } returns ProductWithUnits(Product(id="persisted",barcode="same",name="fixture"),listOf(ProductUnit(id="unit",unitLabel="piece",price=12.0)))
        vm.setBarcode("same");advanceTimeBy(350);runCurrent()
        vm.loadProduct("persisted");runCurrent()
        val checkingAfterLoad=vm.uiState.value.barcodeChecking
        old.resume("obsolete owner");runCurrent()
        assertFalse(checkingAfterLoad)
        assertEquals("persisted",vm.uiState.value.productId)
        assertNull(vm.uiState.value.barcodeConflict)
        assertFalse(vm.uiState.value.barcodeChecking)
    }

    @Test fun typingDebouncesToLatestValueAndClearingCancelsPendingCheck()=runTest {
        coEvery {repository.getBarcodeConflict(any(),any())} returns null
        vm.setBarcode("1");advanceTimeBy(200)
        vm.setBarcode("12");advanceTimeBy(200)
        vm.setBarcode("123");advanceUntilIdle()
        coVerify(exactly=1){repository.getBarcodeConflict("123",null)}
        coVerify(exactly=0){repository.getBarcodeConflict("1",any());repository.getBarcodeConflict("12",any())}
        vm.setBarcode("cancelled");vm.setBarcode("");advanceUntilIdle()
        coVerify(exactly=0){repository.getBarcodeConflict("cancelled",any())}
        assertFalse(vm.uiState.value.barcodeChecking)
        assertNull(vm.uiState.value.barcodeConflict)
    }
    @Test fun editingExcludesThePersistedProductFromItsOwnBarcodeCheck()=runTest {
        coEvery {repository.getProductById("persisted")} returns ProductWithUnits(Product(id="persisted",name="fixture",barcode="same"),listOf(ProductUnit(id="unit",unitLabel="piece",price=12.0)))
        coEvery {repository.getBarcodeConflict("same","persisted")} returns null
        vm.loadProduct("persisted");advanceUntilIdle()
        vm.setBarcode("same");advanceUntilIdle()
        coVerify(exactly=1){repository.getBarcodeConflict("same","persisted")}
        assertTrue(vm.uiState.value.isValid)
    }
    @Test fun failedCheckCanBeRetriedAndConflictClearsWhenBarcodeIsRemoved()=runTest {
        coEvery {repository.getBarcodeConflict("123",null)} throws java.io.IOException("fixture")
        vm.setName("fixture");vm.updateUnitPrice(0,"12")
        vm.setBarcode("123");advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        coEvery {repository.getBarcodeConflict("123",null)} returns "TAKEN"
        vm.setBarcode("123")
        assertNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isValid)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.barcodeConflict.orEmpty().contains("TAKEN"))
        assertFalse(vm.uiState.value.isValid)
        vm.setBarcode("");advanceUntilIdle()
        assertNull(vm.uiState.value.barcodeConflict)
        assertTrue(vm.uiState.value.isValid)
    }
}
