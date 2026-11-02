package com.trader.salesmanager.ui.customers.addedit
import androidx.lifecycle.ViewModelStore
import com.trader.core.domain.model.Customer
import com.trader.core.domain.repository.CustomerRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CustomerFormTest {
    private val repo=mockk<CustomerRepository>(relaxed=true)
    private val store=ViewModelStore()
    private fun model()=AddEditCustomerViewModel(repo).also { store.put("customer",it) }
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher());coEvery { repo.getPhoneConflict(any(),any()) } returns null }
    @After fun cleanup() { store.clear();Dispatchers.resetMain() }
    @Test fun creationTrimsNameAndPreservesArabic()=runTest {
        val vm=model();vm.updateName("  أحمد محمد  ");vm.save();runCurrent()
        coVerify { repo.insertCustomer(match { it.name=="أحمد محمد" && it.phone=="" }) }
        assertTrue(vm.uiState.value.isSaved)
    }
    @Test fun editUpdatesExistingCustomerRatherThanInserting()=runTest {
        coEvery { repo.getCustomerById(7) } returns Customer(id=7,name="original")
        val vm=model();vm.loadCustomer(7);runCurrent();vm.updateName("edited");vm.save();runCurrent()
        coVerify { repo.updateCustomer(match { it.id==7L && it.name=="edited" }) }
        coVerify(exactly=0) { repo.insertCustomer(any()) }
    }
    @Test fun shortAndReservedNamesNeverPersist()=runTest {
        val vm=model()
        for(name in listOf("","a","زبون زائر","WALK-IN")) { vm.updateName(name);vm.save();runCurrent() }
        coVerify(exactly=0) { repo.insertCustomer(any()) }
    }
    @Test fun conflictingPhoneBlocksSave()=runTest {
        coEvery { repo.getPhoneConflict("0501234567",any()) } returns "existing fixture"
        val vm=model();vm.updateName("fixture");vm.updatePhone("0501234567");advanceUntilIdle();vm.save();runCurrent()
        assertNotNull(vm.uiState.value.phoneConflict)
        coVerify(exactly=0) { repo.insertCustomer(any()) }
    }
    @Test fun rapidDoubleSaveCannotCreateDuplicateCustomer()=runTest {
        coEvery { repo.insertCustomer(any()) } coAnswers { kotlinx.coroutines.delay(100);7 }
        val vm=model();vm.updateName("fixture");vm.save();vm.save();advanceUntilIdle()
        coVerify(exactly=1) { repo.insertCustomer(any()) }
    }
    @Test fun phoneInputKeepsOnlyTenDigits()=runTest {
        val vm=model();vm.updatePhone("05x0123-456789")
        assertEquals("0501234567",vm.uiState.value.phone)
    }
}
