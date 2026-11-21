package com.trader.salesmanager.sales

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionViewModel
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
class SaleSessionViewModelTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private lateinit var db:AppDatabase
    private val actor=MutableStateFlow("")
    private val activation=mockk<ActivationRepository>()
    private val tx=mockk<TransactionRepository>(relaxed=true)
    private val customers=mockk<CustomerRepository>()
    private val invoice=mockk<InvoiceItemRepository>(relaxed=true)
    private val customer=Customer(id=1,name="fixture")
    private fun open() { db=Room.databaseBuilder(context,AppDatabase::class.java,"sale-session.db").allowMainThreadQueries().build() }
    @Before fun setup()=runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        context.deleteDatabase("sale-session.db");open()
        db.customerDao().upsertCustomer(CustomerEntity(id=1,name="fixture",createdAt=0))
        db.cashBoxDao().upsert(CashBoxEntity("1",1,"cash",currentBalance=100.0,merchantId="merchant-a",updatedAt=0))
        coEvery { activation.getMerchantCode() } answers { actor.value }
        every { customers.getAllCustomers() } returns flowOf(listOf(customer))
        Unit
    }
    @After fun close() { db.close();context.deleteDatabase("sale-session.db");Dispatchers.resetMain() }
    private fun viewModel(state:SavedStateHandle)=AddEditTransactionViewModel(tx,customers,mockk(relaxed=true),mockk(relaxed=true),invoice,
        activation,SaleCommands(db,authorizedMerchant={actor.value}),state).apply {
            selectCustomer(customer);updateAmount("50");selectPaymentMethod(PaymentMethod(id=1,name="cash"))
        }
    private suspend fun awaitResult(vm:AddEditTransactionViewModel)=withTimeout(10000) { vm.uiState.first { it.isSaved || it.error!=null } }
    @Test fun viewModelCreatedBeforeActivationUsesCurrentMerchantWhenSaving()=runBlocking {
        val vm=viewModel(SavedStateHandle())
        try {
            actor.value="merchant-a";vm.save();val result=awaitResult(vm)
            assertTrue(result.error.orEmpty(),result.isSaved)
            assertEquals("merchant-a",db.saleOutboxDao().all().single().merchantId)
            assertEquals(150.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
            coVerify(exactly=0) { tx.insertTransaction(any()) }
            coVerify(exactly=0) { invoice.saveItems(any()) }
        } finally { vm.viewModelScope.cancel() }
    }
    @Test fun restoredDraftCannotRetargetMerchantAndOriginalAccountRetryIsIdempotent()=runBlocking {
        val state=SavedStateHandle();actor.value="merchant-a";val first=viewModel(state)
        first.save();assertTrue(awaitResult(first).isSaved);first.viewModelScope.cancel()
        val restored=SavedStateHandle(state.keys().associateWith { state.get<Any?>(it) })
        db.close();open();actor.value="merchant-b"
        val next=viewModel(restored)
        try {
            next.save();assertFalse(awaitResult(next).isSaved)
            assertEquals(1,db.saleOutboxDao().all().size)
            actor.value="merchant-a";next.save();assertTrue(withTimeout(10000) { next.uiState.first { it.isSaved } }.isSaved)
            assertEquals(1,db.saleOutboxDao().all().size)
            assertEquals(150.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
        } finally { next.viewModelScope.cancel() }
    }
}
