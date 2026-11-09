package com.trader.salesmanager.ui.qualification

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.CustomerEntity
import com.trader.core.data.local.entity.TransactionEntity
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.debts.DebtsViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class DebtRefreshRoomTest {
    private lateinit var db:AppDatabase;private val store=ViewModelStore()
    @Before fun open(){Dispatchers.setMain(Dispatchers.Unconfined);db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()}
    @After fun close(){store.clear();db.close();Dispatchers.resetMain()}
    @Test fun openDebtScreenReflectsSalePaymentAndDeletionWithoutCustomerMutation()=runBlocking {
        db.customerDao().insertCustomer(CustomerEntity(7,"fixture"))
        val customers=mockk<CustomerRepository>();val transactions=mockk<TransactionRepository>()
        every{customers.getAllCustomers()} returns db.customerDao().getAllCustomers().map{rows->rows.map{it.toDomain()}}
        every{transactions.getAllTransactions()} returns db.transactionDao().getAllTransactions().map{rows->rows.map{it.toDomain()}}
        coEvery{transactions.getUnpaidAmountByCustomer(any())} coAnswers{db.transactionDao().getUnpaidAmountByCustomer(firstArg())}
        val vm=DebtsViewModel(customers,transactions).also{store.put("debt",it)}
        assertTrue(withTimeout(5000){vm.uiState.first{!it.isLoading}}.debts.isEmpty())
        val id=db.transactionDao().insertTransaction(TransactionEntity(customerId=7,amount=100.0,isPaid=false))
        val updated=withTimeoutOrNull(1500){vm.uiState.first{it.debts.singleOrNull()?.debt==100.0}}
        assertNotNull("Open debt screen must react to transaction changes even when customers are unchanged",updated)
        db.transactionDao().updateTransaction(db.transactionDao().getTransactionById(id)!!.copy(isPaid=true))
        assertTrue(withTimeout(5000){vm.uiState.first{it.debts.isEmpty()}}.debts.isEmpty())
        db.transactionDao().updateTransaction(db.transactionDao().getTransactionById(id)!!.copy(isPaid=false))
        withTimeout(5000){vm.uiState.first{it.debts.singleOrNull()?.debt==100.0}}
        db.transactionDao().deleteById(id)
        assertTrue(withTimeout(5000){vm.uiState.first{it.debts.isEmpty()}}.debts.isEmpty())
    }
}
