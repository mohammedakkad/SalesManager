package com.trader.core.data.repository
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.StockRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class InventoryIntegrityTest {
 private lateinit var db:AppDatabase
 private val stock=mockk<StockRepository>()
 private fun repo()=InventoryRepositoryImpl(db.inventoryDao(),db.productDao(),stock,"m",db)
 @Before fun open() {
  db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
  coEvery { stock.manualAdjust(any(),any(),any(),any(),any(),any(),any()) } coAnswers {
   val unit=secondArg<String>();db.productDao().updateQuantity(unit,db.productDao().getQuantity(unit)!!+thirdArg<Double>())
  }
 }
 @After fun close(){db.close()}
 private suspend fun seed() {
  db.productDao().insertProduct(Product(id="p",merchantId="m",name="fixture").toEntity())
  db.productDao().insertUnits(listOf("a","b").map { ProductUnit(id=it,productId="p",quantityInStock=10.0).toEntity() })
  db.inventoryDao().insertSession(InventorySession(id="s",merchantId="m",startedAt=123).toEntity())
  db.inventoryDao().insertSessionItems(listOf("a","b").map { InventorySessionItem(id=it,sessionId="s",productId="p",productName=it,unitId=it,unitLabel="piece",systemQuantity=10.0,actualQuantity=12.0).toEntity() })
 }
 @Test fun repeatedFinalizationAppliesEachAdjustmentOnce()=runTest {
  seed();val r=repo();r.finishSession("s");r.finishSession("s")
  assertEquals(12.0,db.productDao().getQuantity("a")!!,0.0)
  coVerify(exactly=2){stock.manualAdjust(any(),any(),any(),any(),any(),any(),any())}
 }
 @Test fun failureOnSecondUnitRollsBackFirstQuantityAndSession()=runTest {
  seed();coEvery { stock.manualAdjust(any(),"b",any(),any(),any(),any(),any()) } throws IOException("fixture failure")
  try {repo().finishSession("s");fail("Failure swallowed")}catch(expected:IOException){}
  assertEquals(10.0,db.productDao().getQuantity("a")!!,0.0)
  assertEquals("IN_PROGRESS",db.inventoryDao().getAllSessionsOnce().single().status)
 }
 @Test fun finishPreservesOriginalStartTimeAndMerchant()=runTest {
  seed();repo().finishSession("s");val s=db.inventoryDao().getAllSessionsOnce().single()
  assertEquals(123L,s.startedAt);assertEquals("m",s.merchantId);assertEquals(2,s.totalAdjustments)
 }
 @Test fun cancellationPreservesHistoryWithoutChangingStock()=runTest {
  seed();repo().cancelSession("s");val s=db.inventoryDao().getAllSessionsOnce().single()
  assertEquals(123L,s.startedAt);assertEquals(10.0,db.productDao().getQuantity("a")!!,0.0)
 }
 @Test fun newSessionCapturesCurrentProductUnits()=runTest {
  seed();val id=repo().startNewSession("m")
  assertEquals(2,db.inventoryDao().getSessionItemsOnce(id).size)
 }
 @Test fun invalidNegativeCountNeverPersists()=runTest {
  seed();val item=db.inventoryDao().getSessionItemsOnce("s").first().toDomain().copy(actualQuantity=-1.0)
  try {repo().updateSessionItem(item);fail("Negative count accepted")}catch(expected:IllegalArgumentException){}
  assertEquals(12.0,db.inventoryDao().getSessionItemsOnce("s").first().actualQuantity!!,0.0)
 }
 @Test fun wrongMerchantCannotFinalizeLocalSession()=runTest {
  seed();val s=db.inventoryDao().getAllSessionsOnce().single();db.inventoryDao().updateSession(s.copy(merchantId="other"))
  try {repo().finishSession("s");fail("Wrong merchant accepted")}catch(expected:IllegalArgumentException){}
  assertEquals(10.0,db.productDao().getQuantity("a")!!,0.0)
 }
 @Test fun uncountedAndUnchangedItemsDoNotAdjustStock()=runTest {
  seed();db.inventoryDao().getSessionItemsOnce("s").forEach { db.inventoryDao().updateSessionItem(it.copy(actualQuantity=if(it.id=="a") null else 10.0)) }
  repo().finishSession("s");coVerify(exactly=0){stock.manualAdjust(any(),any(),any(),any(),any(),any(),any())}
 }
}
