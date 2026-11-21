package com.trader.salesmanager.ui.qualification

import androidx.lifecycle.ViewModelStore
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.EmployeeRepository
import com.trader.salesmanager.ui.employees.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class EmployeeCommandsTest {
    private val repo=mockk<EmployeeRepository>(relaxed=true);private val store=ViewModelStore()
    @Before fun setup() {Dispatchers.setMain(StandardTestDispatcher());every {repo.observeEmployees()} returns flowOf(emptyList());coEvery {repo.getByPin(any())} returns null}
    @After fun cleanup(){store.clear();Dispatchers.resetMain()}
    private fun model()=EmployeeManagementViewModel(repo).also{store.put("employee",it)}
    @Test fun invalidNameAndPinNeverPersistAndInputIsSanitized()=runTest {
        val vm=model();runCurrent();vm.submit();assertEquals(AddEmployeeError.NAME_BLANK,vm.form.value.error)
        vm.onNameChange("سارة");vm.onPinChange("1a2-345");assertEquals("1234",vm.form.value.pin)
        vm.onPinChange("12");vm.submit();assertEquals(AddEmployeeError.PIN_INVALID,vm.form.value.error);coVerify(exactly=0){repo.addOrUpdate(any())}
    }
    @Test fun duplicatePinIsRejectedWithoutOverwritingExistingEmployee()=runTest {
        coEvery{repo.getByPin("1234")} returns Employee(name="آخر",pinCode="1234")
        val vm=model();vm.onNameChange("سارة");vm.onPinChange("1234");vm.submit();runCurrent()
        assertEquals(AddEmployeeError.PIN_TAKEN,vm.form.value.error);assertFalse(vm.form.value.submitting);coVerify(exactly=0){repo.addOrUpdate(any())}
    }
    @Test fun successfulSubmissionTrimsNamePreservesRoleAndResetsForm()=runTest {
        val vm=model();vm.onNameChange("  سارة  ");vm.onPinChange("5678");vm.onRoleChange(EmployeeRole.ADMIN);assertTrue(vm.form.value.canSubmit)
        vm.submit();runCurrent();coVerify(exactly=1){repo.addOrUpdate(match{it.name=="سارة"&&it.pinCode=="5678"&&it.role==EmployeeRole.ADMIN})}
        assertEquals(AddEmployeeForm(),vm.form.value);assertEquals(EmployeeManagementEvent.EmployeeAdded,vm.events.first())
    }
    @Test fun persistenceFailureKeepsFormForRetryAndShowsFailure()=runTest {
        coEvery{repo.addOrUpdate(any())} throws java.io.IOException("fixture storage failure")
        val vm=model();vm.onNameChange("سارة");vm.onPinChange("5678");vm.submit();runCurrent()
        assertEquals("سارة",vm.form.value.name);assertFalse(vm.form.value.submitting);assertEquals(EmployeeManagementEvent.Failure("fixture storage failure"),vm.events.first())
    }
    @Test fun seededManagerCannotBeDeletedButCashierCan()=runTest {
        val vm=model();vm.delete(Employee(id="admin_m1",name="مدير",pinCode="1234",role=EmployeeRole.ADMIN));runCurrent()
        assertTrue(vm.events.first() is EmployeeManagementEvent.Failure);coVerify(exactly=0){repo.deleteById(any())}
        vm.delete(Employee(id="cashier",name="بائع",pinCode="5678"));runCurrent();coVerify {repo.deleteById("cashier")};assertEquals(EmployeeManagementEvent.EmployeeDeleted,vm.events.first())
    }
}
