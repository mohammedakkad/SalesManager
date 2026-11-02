package com.trader.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.CustomerEntity
import com.trader.core.data.local.entity.TransactionEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class StaleTransactionAckTest {
 private lateinit var db:AppDatabase
 @Before fun open(){db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()}
 @After fun close(){db.close()}
 private suspend fun TestScope.exercise(failure:Boolean, edited:Boolean=true){
  db.customerDao().insertCustomer(CustomerEntity(id=7,name="fixture"))
  db.transactionDao().insertTransaction(TransactionEntity(id=8,customerId=7,amount=10.0,isPaid=false,syncStatus="PENDING"))
  val sync=mockk<FirebaseSyncService>()
  val activation=mockk<ActivationRepository>()
  every{activation.observeMerchantCode()} returns emptyFlow()
  coEvery{activation.getMerchantCode()} returns "fixture"
  coEvery{sync.pushTransaction(any(),any())} coAnswers {
   val old=db.transactionDao().getTransactionById(8)!!
   if(edited) db.transactionDao().updateTransaction(old.copy(amount=20.0,syncStatus="PENDING"))
   if(failure) throw IOException("old upload failed")
  }
  val repository=TransactionRepositoryImpl(db.transactionDao(),db.customerDao(),db.paymentMethodDao(),sync,activation,mockk(relaxed=true),mockk(relaxed=true),mockk(relaxed=true),db.returnDao(),backgroundScope)
  repository.syncPendingTransactions()
  val current=db.transactionDao().getTransactionById(8)!!
  assertEquals(if(edited)20.0 else 10.0,current.amount,0.0)
  assertEquals(if(edited) "PENDING" else if(failure) "FAILED" else "SYNCED",current.syncStatus)
 }
 @Test fun unchangedSuccessAcknowledges()=runTest{exercise(false,false)}
 @Test fun unchangedFailureRemainsRetryable()=runTest{exercise(true,false)}
 @Test fun oldSuccessCannotAcknowledgeNewEdit()=runTest{exercise(false)}
 @Test fun oldFailureCannotMarkNewEditFailed()=runTest{exercise(true)}
}
