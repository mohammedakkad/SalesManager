package com.trader.core.data.repository
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ActivationRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@OptIn(ExperimentalCoroutinesApi::class)
class CustomerSyncIntegrityTest {
 private lateinit var db:AppDatabase
 private val activation=mockk<ActivationRepository>()
 private val sync=mockk<FirebaseSyncService>()
 private val code=MutableStateFlow("m")
 private val remote=MutableSharedFlow<List<Customer>>()
 @Before fun open(){
  db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
  every {activation.observeMerchantCode()} returns code
  every {sync.observeCustomers(any())} returns remote
 }
 @After fun close(){db.close()}
 private fun TestScope.repo()=CustomerRepositoryImpl(db.customerDao(),db.transactionDao(),sync,activation,backgroundScope)
 private suspend fun awaitSentinel()=withContext(Dispatchers.IO){withTimeout(5000){db.customerDao().getAllCustomers().first { rows->rows.any {it.id==99L} }}}
 @Test fun remoteSnapshotPreservesEveryUnsyncedCustomerStatus()=runTest {
  repo();runCurrent()
  for((id,status) in listOf(1L to SyncStatus.PENDING,2L to SyncStatus.FAILED,3L to SyncStatus.CONFLICT)) db.customerDao().upsertCustomer(CustomerEntity(id=id,name="local",createdAt=0,syncStatus=status.name))
  remote.emit((1L..3L).map {Customer(id=it,name="remote",syncStatus=SyncStatus.SYNCED)}+Customer(id=99,name="sentinel"));awaitSentinel()
  for(id in 1L..3L) assertEquals("local",db.customerDao().getCustomerById(id)!!.name)
 }
 @Test fun logoutCancelsCustomerSubscription()=runTest {
  repo();runCurrent();assertEquals(1,remote.subscriptionCount.value);code.value="";runCurrent();assertEquals(0,remote.subscriptionCount.value)
 }
 @Test fun syncedRemoteEditsPreserveCustomerTransactionHistory()=runTest {
  repo();runCurrent();db.customerDao().upsertCustomer(CustomerEntity(id=7,name="old",createdAt=0))
  db.transactionDao().insertTransaction(TransactionEntity(id=8,customerId=7,amount=35.0,isPaid=false))
  remote.emit(listOf(Customer(id=7,name="updated"),Customer(id=99,name="sentinel")));awaitSentinel()
  assertEquals("updated",db.customerDao().getCustomerById(7)!!.name)
  assertEquals(35.0,db.transactionDao().getUnpaidAmountByCustomer(7),0.0)
 }
 @Test fun visitorCannotBeOverwrittenByRemoteSnapshot()=runTest {
  repo();runCurrent();db.customerDao().upsertCustomer(CustomerEntity(id=-1,name="visitor",createdAt=0))
  remote.emit(listOf(Customer(id=-1,name="bad"),Customer(id=99,name="sentinel")));awaitSentinel()
  assertEquals("visitor",db.customerDao().getCustomerById(-1)!!.name)
 }
}
