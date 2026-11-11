package com.trader.salesmanager.sales

import androidx.lifecycle.viewModelScope
import com.trader.core.domain.repository.ProductRepository
import com.trader.salesmanager.ui.inventory.addedit.AddEditProductViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProductSubmissionTest {
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun cleanup() { Dispatchers.resetMain() }
    @Test fun rapidSaveAndSaveAfterSuccessSubmitOnlyOnce() = runTest {
        val repository=mockk<ProductRepository>()
        val gate=CompletableDeferred<Unit>()
        coEvery { repository.saveProduct(any(),any(),any()) } coAnswers { gate.await(); "saved-id" }
        val vm=AddEditProductViewModel(repository)
        try {
            vm.setName("fixture");vm.updateUnitPrice(0,"12")
            vm.save();vm.save();runCurrent()
            coVerify(exactly=1) { repository.saveProduct(any(),any(),any()) }
            assertTrue(vm.uiState.value.isSaving)
            gate.complete(Unit);advanceUntilIdle()
            assertTrue(vm.uiState.value.savedSuccessfully)
            vm.save();advanceUntilIdle()
            coVerify(exactly=1) { repository.saveProduct(any(),any(),any()) }
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun failedSavePreservesDraftAndAllowsOneControlledRetry() = runTest {
        val repository=mockk<ProductRepository>()
        coEvery { repository.saveProduct(any(),any(),any()) } throws IllegalStateException("offline fixture")
        val vm=AddEditProductViewModel(repository)
        try {
            vm.setName("fixture");vm.updateUnitPrice(0,"12")
            vm.save();advanceUntilIdle()
            assertFalse(vm.uiState.value.isSaving)
            assertFalse(vm.uiState.value.savedSuccessfully)
            assertEquals("fixture",vm.uiState.value.name)
            assertEquals("12",vm.uiState.value.units.single().price)
            assertTrue(vm.uiState.value.error.orEmpty().contains("offline fixture"))
            coEvery { repository.saveProduct(any(),any(),any()) } returns "saved-id"
            vm.save();vm.save();advanceUntilIdle()
            assertTrue(vm.uiState.value.savedSuccessfully)
            assertNull(vm.uiState.value.error)
            coVerify(exactly=2) { repository.saveProduct(any(),any(),any()) }
        } finally { vm.viewModelScope.cancel() }
    }
}
