package com.trader.salesmanager.ui.activation
import androidx.lifecycle.ViewModelStore
import com.trader.core.data.remote.ValidationResult
import com.trader.core.domain.model.StartupStatus
import com.trader.core.domain.repository.ActivationRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ActivationViewModelTest {
    private lateinit var repo:ActivationRepository
    private val store=ViewModelStore()
    private var nextId=0
    private fun model()=ActivationViewModel(repo).also { store.put("activation-${nextId++}",it) }
    @Before fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        repo=mockk(relaxed=true)
        every { repo.observeMerchantStatus() } returns flowOf(null)
        coEvery { repo.verifyStatusOnStartup() } returns StartupStatus.NOT_ACTIVATED
    }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    @Test fun activeAccountProceeds() = runTest {
        coEvery { repo.verifyStatusOnStartup() } returns StartupStatus.ACTIVE
        val vm=model();runCurrent()
        assertEquals(StartupState.Proceed,vm.startupState.value)
    }
    @Test fun disabledAndExpiredAccountsAreBlocked() = runTest {
        for(status in listOf(StartupStatus.DISABLED,StartupStatus.EXPIRED,StartupStatus.DELETED)) {
            coEvery { repo.verifyStatusOnStartup() } returns status
            val vm=model();runCurrent()
            assertTrue(vm.startupState.value is StartupState.Blocked)
            assertFalse((vm.startupState.value as StartupState.Blocked).canRetry)
        }
    }
    @Test fun blankCodeDoesNotCallRemoteOrSaveActivation() = runTest {
        val vm=model();runCurrent();vm.updateCode("  ");vm.activate();runCurrent()
        coVerify(exactly=0) { repo.validateCodeDetailed(any()) }
        coVerify(exactly=0) { repo.saveActivationStatus(any(),any()) }
    }
    @Test fun validCodeIsTrimmedAndSavedOnlyAfterRemoteValidation() = runTest {
        coEvery { repo.validateCodeDetailed("fixture") } returns ValidationResult.Active
        val vm=model();runCurrent();vm.updateCode(" fixture ");vm.activate();runCurrent()
        assertTrue(vm.uiState.value.isSuccess);assertFalse(vm.uiState.value.isLoading)
        coVerifyOrder { repo.validateCodeDetailed("fixture");repo.saveActivationStatus(true,"fixture") }
    }
    @Test fun rejectedCodesNeverPersistActivation() = runTest {
        for(result in listOf(ValidationResult.Disabled,ValidationResult.Expired,ValidationResult.NotFound)) {
            coEvery { repo.validateCodeDetailed("fixture") } returns result
            val vm=model();runCurrent();vm.updateCode("fixture");vm.activate();runCurrent()
            assertFalse(vm.uiState.value.isSuccess);assertFalse(vm.uiState.value.isLoading)
            assertNotNull(vm.uiState.value.error)
        }
        coVerify(exactly=0) { repo.saveActivationStatus(any(),any()) }
    }
    @Test fun networkFailureOffersRetryWithoutActivating() = runTest {
        coEvery { repo.validateCodeDetailed("fixture") } returns ValidationResult.NetworkError
        val vm=model();runCurrent();vm.updateCode("fixture");vm.activate();runCurrent()
        assertTrue(vm.uiState.value.showNoInternetSnackbar)
        assertFalse(vm.uiState.value.isLoading);assertFalse(vm.uiState.value.isSuccess)
        vm.onSnackbarShown();assertFalse(vm.uiState.value.showNoInternetSnackbar)
        coVerify(exactly=0) { repo.saveActivationStatus(any(),any()) }
    }
}
