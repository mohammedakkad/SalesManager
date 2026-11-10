package com.trader.core.data.repository

import java.io.IOException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.CashBoxEntity
import com.trader.core.data.local.entity.CashBoxMovementEntity
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
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class CashBoxIntegrityTest {
    private lateinit var db: AppDatabase
    private val sync = mockk<FirebaseSyncService>()
    private val activation = mockk<ActivationRepository>()
    private val code = MutableStateFlow("m")
    private val boxes = MutableSharedFlow<List<CashBox>>()
    private val movements = MutableSharedFlow<List<CashBoxMovement>>()

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        every { activation.observeMerchantCode() } returns code
        coEvery { activation.getMerchantCode() } answers { code.value }
        every { sync.observeCashBoxes(any()) } returns boxes
        every { sync.observeCashBoxMovements(any()) } returns movements
        coEvery { sync.pushCashBox(any(), any()) } just Runs
        coEvery { sync.pushCashBoxMovement(any(), any()) } just Runs
    }
    @After fun close() { db.close() }
    private fun TestScope.repo() = CashBoxRepositoryImpl(
        db, db.cashBoxDao(), db.cashBoxMovementDao(), db.paymentMethodDao(), sync, activation, backgroundScope
    )
    private suspend fun seed(status: String = "PENDING") {
        for (id in 1L..2L) db.cashBoxDao().upsert(CashBoxEntity(
            id = id.toString(), paymentMethodId = id, paymentMethodName = "fixture$id",
            currentBalance = 100.0, merchantId = "m", syncStatus = status
        ))
    }
    private suspend fun seedMovement(status: String) {
        db.cashBoxMovementDao().insert(CashBoxMovementEntity(
            id = "movement", cashBoxId = "1", paymentMethodName = "fixture1",
            type = CashBoxMovementType.TRANSACTION_EFFECT.name, amountDelta = 10.0,
            balanceAfter = 100.0, createdAt = 1, merchantId = "m", syncStatus = status
        ))
    }
    @Test fun failedRecordsRetryWithoutReapplyingBalancesOrConflicts() = runTest {
        seed("FAILED"); seedMovement("FAILED")
        db.cashBoxDao().upsert(CashBoxEntity("3", 3, "conflict", currentBalance = 25.0, syncStatus = "CONFLICT"))
        val r = repo(); r.syncPendingBoxes(); r.syncPendingBoxes()
        assertEquals("SYNCED", db.cashBoxDao().getById("1")!!.syncStatus)
        assertEquals("SYNCED", db.cashBoxMovementDao().getById("movement")!!.syncStatus)
        assertEquals("CONFLICT", db.cashBoxDao().getById("3")!!.syncStatus)
        assertEquals(100.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
        assertEquals(1, db.cashBoxMovementDao().getAllOnce().size)
        coVerify(exactly = 2) { sync.pushCashBox("m", any()) }
        coVerify(exactly = 1) { sync.pushCashBoxMovement("m", match { it.id == "movement" }) }
    }
    @Test fun cancelledBoxUploadPropagatesWithoutMarkingFailed() = runTest {
        seed(); val r = repo()
        coEvery { sync.pushCashBox(any(), any()) } throws CancellationException("fixture cancellation")
        try { r.syncPendingBoxes(); fail("Cancellation swallowed") } catch (expected: CancellationException) { }
        assertEquals("PENDING", db.cashBoxDao().getById("1")!!.syncStatus)
    }
    @Test fun cancelledMovementUploadPropagatesWithoutMarkingFailed() = runTest {
        seed("SYNCED"); seedMovement("PENDING"); val r = repo()
        coEvery { sync.pushCashBoxMovement(any(), any()) } throws CancellationException("fixture cancellation")
        try { r.syncPendingBoxes(); fail("Cancellation swallowed") } catch (expected: CancellationException) { }
        assertEquals("PENDING", db.cashBoxMovementDao().getById("movement")!!.syncStatus)
    }
    @Test fun logoutCancelsBothRemoteSubscriptions() = runTest {
        repo(); runCurrent()
        assertEquals(1, boxes.subscriptionCount.value); assertEquals(1, movements.subscriptionCount.value)
        code.value = "next"; runCurrent()
        assertEquals(1, boxes.subscriptionCount.value); assertEquals(1, movements.subscriptionCount.value)
        verify(exactly = 1) { sync.observeCashBoxes("next") }
        verify(exactly = 1) { sync.observeCashBoxMovements("next") }
        code.value = ""; runCurrent()
        assertEquals(0, boxes.subscriptionCount.value); assertEquals(0, movements.subscriptionCount.value)
    }
    @Test fun secondBoxFailureRollsBackWholePaymentTransfer() = runTest {
        seed(); val r = repo()
        db.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER reject_second_box BEFORE INSERT ON cash_box_movements
            WHEN NEW.cashBoxId = '2' BEGIN SELECT RAISE(ABORT, 'fixture failure'); END
        """.trimIndent())
        try { r.applyTransactionEffect(42, 1, 30.0, true, 2, 30.0, true); fail("Fixture trigger did not fire") }
        catch (expected: android.database.sqlite.SQLiteConstraintException) { }
        assertEquals(100.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
        assertEquals(100.0, db.cashBoxDao().getById("2")!!.currentBalance, 0.0)
        assertTrue(db.cashBoxMovementDao().getAllOnce().isEmpty())
    }
    @Test fun paidTransferReconcilesBalancesAndLedger() = runTest {
        seed(); repo().applyTransactionEffect(42, 1, 30.0, true, 2, 30.0, true)
        assertEquals(70.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
        assertEquals(130.0, db.cashBoxDao().getById("2")!!.currentBalance, 0.0)
        val entries = db.cashBoxMovementDao().getAllOnce()
        assertEquals(2, entries.size); assertEquals(0.0, entries.sumOf { it.amountDelta }, 0.0)
        assertTrue(entries.all { it.relatedTransactionId == 42L })
    }
    @Test fun unchangedPaymentDoesNotCreateLedgerNoise() = runTest {
        seed(); repo().applyTransactionEffect(42, 1, 30.0, true, 1, 30.0, true)
        assertEquals(100.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
        assertTrue(db.cashBoxMovementDao().getAllOnce().isEmpty())
    }
    @Test fun unpaidSaleDoesNotAffectCash() = runTest {
        seed(); repo().applyTransactionEffect(42, null, 0.0, false, 1, 50.0, false)
        assertEquals(100.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
        assertTrue(db.cashBoxMovementDao().getAllOnce().isEmpty())
    }

    @Test fun initialBalanceCanBeSetOnlyOnce() = runTest {
        db.cashBoxDao().upsert(CashBoxEntity("1", 1, "fixture", merchantId = "m"))
        val r = repo(); r.setInitialBalance("1", 25.0); r.setInitialBalance("1", 40.0)
        val box = db.cashBoxDao().getById("1")!!
        assertEquals(25.0, box.currentBalance, 0.0)
        assertEquals(25.0, box.initialBalance, 0.0); assertNotNull(box.initialBalanceSetAt)
        val ledger = db.cashBoxMovementDao().getAllOnce().single()
        assertEquals(CashBoxMovementType.INITIAL_BALANCE.name, ledger.type)
        assertEquals(25.0, ledger.amountDelta, 0.0); assertEquals(25.0, ledger.balanceAfter, 0.0)
    }
    @Test fun manualCorrectionRecordsOnlyDifferenceAndKeepsInitialHistory() = runTest {
        db.cashBoxDao().upsert(CashBoxEntity("1", 1, "fixture", merchantId = "m"))
        val r = repo(); r.setInitialBalance("1", 25.0)
        r.adjustBalance("1", 40.0, "count fixture", AdjustmentReason.COUNT)
        r.adjustBalance("1", 40.0, "unchanged", AdjustmentReason.COUNT)
        val box = db.cashBoxDao().getById("1")!!
        assertEquals(40.0, box.currentBalance, 0.0); assertEquals(25.0, box.initialBalance, 0.0)
        val entries = db.cashBoxMovementDao().getAllOnce(); assertEquals(2, entries.size)
        val correction = entries.single { it.type == CashBoxMovementType.MANUAL_ADJUSTMENT.name }
        assertEquals(15.0, correction.amountDelta, 0.0); assertEquals(40.0, correction.balanceAfter, 0.0)
        assertEquals("count fixture", correction.note)
    }
    @Test fun offlineRetryKeepsDurableRecordsWithoutRemoteRequests() = runTest {
        seed("FAILED"); seedMovement("FAILED"); code.value = ""
        repo().syncPendingBoxes()
        assertEquals("FAILED", db.cashBoxDao().getById("1")!!.syncStatus)
        assertEquals("FAILED", db.cashBoxMovementDao().getById("movement")!!.syncStatus)
        coVerify(exactly = 0) { sync.pushCashBox(any(), any()) }
        coVerify(exactly = 0) { sync.pushCashBoxMovement(any(), any()) }
    }
    @Test fun partialBatchFailureDoesNotBlockOthersAndRetryUsesSameLedgerId() = runTest {
        seed(); seedMovement("PENDING"); val r = repo()
        coEvery { sync.pushCashBox("m", match { it.id == "1" }) } throws IOException("offline fixture")
        r.syncPendingBoxes()
        assertEquals("FAILED", db.cashBoxDao().getById("1")!!.syncStatus)
        assertEquals("SYNCED", db.cashBoxDao().getById("2")!!.syncStatus)
        assertEquals("SYNCED", db.cashBoxMovementDao().getById("movement")!!.syncStatus)
        coEvery { sync.pushCashBox("m", match { it.id == "1" }) } just Runs
        r.syncPendingBoxes()
        assertEquals("SYNCED", db.cashBoxDao().getById("1")!!.syncStatus)
        assertEquals(100.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
        assertEquals("movement", db.cashBoxMovementDao().getAllOnce().single().id)
        coVerify(exactly = 2) { sync.pushCashBox("m", match { it.id == "1" }) }
        coVerify(exactly = 1) { sync.pushCashBox("m", match { it.id == "2" }) }
        coVerify(exactly = 1) { sync.pushCashBoxMovement("m", match { it.id == "movement" }) }
    }
    @Test fun infiniteInitialBalanceIsRejectedBeforeAnyRoomWrite()=runTest {
        db.cashBoxDao().upsert(CashBoxEntity("1",1,"fixture",merchantId="m"));val r=repo()
        try{r.setInitialBalance("1",Double.POSITIVE_INFINITY);fail("Infinite initial balance accepted")}catch(expected:IllegalArgumentException){}
        assertEquals(0.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0);assertNull(db.cashBoxDao().getById("1")!!.initialBalanceSetAt)
        assertTrue(db.cashBoxMovementDao().getAllOnce().isEmpty())
    }
    @Test fun nanInitialBalanceIsRejectedAsInputRatherThanASqlConstraintFailure()=runTest {
        db.cashBoxDao().upsert(CashBoxEntity("1",1,"fixture",merchantId="m"));val r=repo()
        try{r.setInitialBalance("1",Double.NaN);fail("NaN accepted")}catch(expected:IllegalArgumentException){}
        assertEquals(0.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0);assertTrue(db.cashBoxMovementDao().getAllOnce().isEmpty())
    }
    @Test fun infiniteCorrectionCannotCorruptACashBoxOrItsLedger()=runTest {
        db.cashBoxDao().upsert(CashBoxEntity("1",1,"fixture",currentBalance=100.0,initialBalanceSetAt=1,merchantId="m"));val r=repo()
        try{r.adjustBalance("1",Double.POSITIVE_INFINITY,"fixture",AdjustmentReason.COUNT);fail("Infinite correction accepted")}catch(expected:IllegalArgumentException){}
        assertEquals(100.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0);assertTrue(db.cashBoxMovementDao().getAllOnce().isEmpty())
    }
    @Test fun overflowingCorrectionDeltaCannotMutateAFiniteExistingBalance()=runTest {
        db.cashBoxDao().upsert(CashBoxEntity("1",1,"fixture",currentBalance=-1e308,initialBalanceSetAt=1,merchantId="m"));val r=repo()
        try{r.adjustBalance("1",1e308,"fixture",AdjustmentReason.COUNT);fail("Overflowing correction accepted")}catch(expected:IllegalArgumentException){}
        assertEquals(-1e308,db.cashBoxDao().getById("1")!!.currentBalance,0.0);assertTrue(db.cashBoxMovementDao().getAllOnce().isEmpty())
    }
    @Test fun overflowingCashEffectRollsBackItsBalanceAndLedger()=runTest {
        db.cashBoxDao().upsert(CashBoxEntity("1",1,"fixture",currentBalance=1e308,initialBalanceSetAt=1,merchantId="m"));val r=repo()
        try{r.applyTransactionEffect(42,null,0.0,false,1,1e308,true);fail("Overflowing cash effect accepted")}catch(expected:IllegalArgumentException){}
        assertEquals(1e308,db.cashBoxDao().getById("1")!!.currentBalance,0.0);assertTrue(db.cashBoxMovementDao().getAllOnce().isEmpty())
    }

}
