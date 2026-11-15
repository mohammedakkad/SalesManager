package com.trader.salesmanager.ui.qualification

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.EmployeeEntity
import com.trader.core.domain.model.Employee
import com.trader.core.domain.repository.EmployeeRepository
import com.trader.salesmanager.ui.employees.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Repository seams retain actual SQLite writes; failures/delivery timing are deterministic. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class EmployeeFailureRoomTest {
    private lateinit var db:AppDatabase
    private val repo=mockk<EmployeeRepository>(relaxed=true)
    private val store=ViewModelStore()
    @Before fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),AppDatabase::class.java)
            .setQueryExecutor { it.run() }.setTransactionExecutor { it.run() }.allowMainThreadQueries().build()
        every { repo.observeEmployees() } returns emptyFlow()
        coEvery { repo.getByPin(any()) } coAnswers { db.employeeDao().getByPin("tenant",firstArg())?.toDomain() }
        coEvery { repo.addOrUpdate(any()) } coAnswers { db.employeeDao().insertOrUpdate(EmployeeEntity.fromDomain(firstArg<Employee>().copy(merchantId="tenant"))) }
        coEvery { repo.deleteById(any()) } coAnswers { db.employeeDao().deleteById(firstArg()) }
    }
    @After fun close() { store.clear();db.close();Dispatchers.resetMain() }
    private fun model()=EmployeeManagementViewModel(repo).also { store.put("employee",it);it.onNameChange("سارة");it.onPinChange("1234") }
    private fun TestScope.events(vm:EmployeeManagementViewModel):MutableList<EmployeeManagementEvent> {
        val result=mutableListOf<EmployeeManagementEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { result+=it } }
        return result
    }
    @Test fun lookupFailureCannotBeInterpretedAsAnUnusedPin()=runTest {
        coEvery { repo.getByPin(any()) } throws java.io.IOException("lookup unavailable")
        val vm=model();val events=events(vm);vm.submit();advanceUntilIdle()
        assertEquals(0,db.employeeDao().countByMerchant("tenant"));coVerify(exactly=0) { repo.addOrUpdate(any()) }
        assertFalse(vm.form.value.submitting);assertEquals("سارة",vm.form.value.name)
        assertEquals(listOf(EmployeeManagementEvent.Failure("lookup unavailable")),events)
    }
    @Test fun lookupCancellationDoesNotCreateEmployeeOrFailureEvent()=runTest {
        var child:Job?=null
        coEvery { repo.getByPin(any()) } coAnswers { child=currentCoroutineContext()[Job];throw CancellationException("lookup cancelled") }
        val vm=model();val events=events(vm);vm.submit();advanceUntilIdle()
        assertEquals(0,db.employeeDao().countByMerchant("tenant"));coVerify(exactly=0) { repo.addOrUpdate(any()) }
        assertTrue(child!!.isCancelled);assertFalse(vm.form.value.submitting);assertEquals("1234",vm.form.value.pin);assertTrue(events.isEmpty())
    }
    @Test fun saveCancellationRetainsDraftAndReleasesSubmittingWithoutFailureEvent()=runTest {
        var child:Job?=null
        coEvery { repo.addOrUpdate(any()) } coAnswers { child=currentCoroutineContext()[Job];throw CancellationException("save cancelled") }
        val vm=model();val events=events(vm);vm.submit();advanceUntilIdle()
        assertTrue(child!!.isCancelled);assertFalse(vm.form.value.submitting);assertEquals("سارة",vm.form.value.name)
        assertTrue(events.isEmpty());assertEquals(0,db.employeeDao().countByMerchant("tenant"))
    }
    @Test fun deleteCancellationCannotBecomeAVisibleDeletionFailure()=runTest {
        val row=Employee(id="cashier",merchantId="tenant",name="fixture",pinCode="5678")
        db.employeeDao().insertOrUpdate(EmployeeEntity.fromDomain(row));var child:Job?=null
        coEvery { repo.deleteById(any()) } coAnswers { child=currentCoroutineContext()[Job];throw CancellationException("delete cancelled") }
        val vm=model();val events=events(vm);vm.delete(row);advanceUntilIdle()
        assertTrue(child!!.isCancelled);assertTrue(events.isEmpty());assertNotNull(db.employeeDao().getById("cashier"))
    }
    @Test fun ordinarySaveFailureStillReportsReasonAndPreservesDraft()=runTest {
        coEvery { repo.addOrUpdate(any()) } throws java.io.IOException("save unavailable")
        val vm=model();val events=events(vm);vm.submit();advanceUntilIdle()
        assertEquals(listOf(EmployeeManagementEvent.Failure("save unavailable")),events)
        assertEquals("سارة",vm.form.value.name);assertFalse(vm.form.value.submitting);assertEquals(0,db.employeeDao().countByMerchant("tenant"))
    }
    @Test fun ordinaryDeleteFailureRetainsRowAndReportsTheFailure()=runTest {
        val row=Employee(id="cashier",merchantId="tenant",name="fixture",pinCode="5678");db.employeeDao().insertOrUpdate(EmployeeEntity.fromDomain(row))
        coEvery { repo.deleteById(any()) } throws java.io.IOException("delete unavailable")
        val vm=model();val events=events(vm);vm.delete(row);advanceUntilIdle()
        assertEquals(listOf(EmployeeManagementEvent.Failure("delete unavailable")),events);assertNotNull(db.employeeDao().getById(row.id))
    }
    @Test fun twoSubmitClicksBeforeDispatchPersistOnlyOneEmployee()=runTest {
        coEvery { repo.getByPin(any()) } coAnswers {
            val found=db.employeeDao().getByPin("tenant",firstArg())?.toDomain()
            yield() // Both legitimate lookup completions can precede either write.
            found
        }
        val vm=model();val events=events(vm);vm.submit();vm.submit();advanceUntilIdle()
        val rows=db.employeeDao().getAllOnce("tenant")
        assertEquals("Double click must not persist two UUID employees with one PIN",1,rows.size)
        coVerify(exactly=1) { repo.addOrUpdate(any()) };assertEquals(listOf(EmployeeManagementEvent.EmployeeAdded),events)
    }
}
