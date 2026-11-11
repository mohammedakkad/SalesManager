package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import androidx.room.Room
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.CashBoxEntity
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.data.repository.CashBoxRepositoryImpl
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.boxes.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/** Production Room database/repository/ViewModel plus actual Compose actions; only remote SDK boundary is fake. */
@RunWith(RobolectricTestRunner::class)
class CashBoxRollbackRoomUiTest:OfflineSellerUiTest() {
    private lateinit var db:AppDatabase
    private lateinit var scope:CoroutineScope
    private lateinit var repo:CashBoxRepositoryImpl
    @Before fun setup(){
        ShadowToast.reset();db=Room.inMemoryDatabaseBuilder(activity,AppDatabase::class.java).allowMainThreadQueries().build()
        scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
        val remote=mockk<FirebaseSyncService>(relaxed=true);val activation=mockk<ActivationRepository>()
        every{activation.observeMerchantCode()} returns flowOf("m");coEvery{activation.getMerchantCode()} returns "m"
        every{remote.observeCashBoxes("m")} returns flowOf(emptyList());every{remote.observeCashBoxMovements("m")} returns flowOf(emptyList())
        repo=CashBoxRepositoryImpl(db,db.cashBoxDao(),db.cashBoxMovementDao(),db.paymentMethodDao(),remote,activation,scope)
    }
    @After fun cleanup(){activity.viewModelStore.clear();runBlocking{scope.coroutineContext[Job]!!.cancelAndJoin()};db.close()}
    private fun seed(initialized:Boolean){runBlocking{
        db.cashBoxDao().upsert(CashBoxEntity("cash",1,"صندوق الغرفة",currentBalance=if(initialized)100.0 else 0.0,
            initialBalance=if(initialized)100.0 else 0.0,initialBalanceSetAt=if(initialized)1 else null,merchantId="m",updatedAt=1,syncStatus="SYNCED"))
    }}
    private fun screen(){
        val methods=mockk<PaymentMethodRepository>();every{methods.getAllPaymentMethods()} returns flowOf(emptyList())
        val vm=owned(BoxesViewModel(repo,methods,activity));show{BoxesScreen({},vm)}
        ui.waitUntil(5000){ui.waitForIdle();!vm.uiState.value.isLoading&&vm.uiState.value.boxes.size==1}
    }
    private fun failLedger(){db.openHelper.writableDatabase.execSQL("CREATE TRIGGER cash_ui_fault BEFORE INSERT ON cash_box_movements BEGIN SELECT RAISE(ABORT, 'cash-ui-fault'); END")}
    private fun waitForFailure(){ui.waitUntil(5000){ui.waitForIdle();ShadowToast.getTextOfLatestToast()?.contains("cash-ui-fault")==true}}
    private fun amount()=ui.onNode(hasSetTextAction() and hasText("المبلغ") and hasAnyAncestor(isDialog()))
    @Test fun initialLedgerFailureRollsBackRoomAndRetainsTheDraftUntilOneSuccessfulRetry(){
        seed(false);failLedger();screen();ui.onNodeWithText("حدد الرصيد الابتدائي").performScrollTo().performClick()
        amount().performTextReplacement("200");ui.onNodeWithText("حفظ").performClick();waitForFailure()
        runBlocking{val box=db.cashBoxDao().getById("cash")!!;assertEquals(0.0,box.currentBalance,0.0);assertNull(box.initialBalanceSetAt);assertTrue(db.cashBoxMovementDao().getAllOnce().isEmpty())}
        amount().assertTextContains("200");db.openHelper.writableDatabase.execSQL("DROP TRIGGER cash_ui_fault")
        ui.onNodeWithText("حفظ").performClick();ui.waitUntil(5000){ui.waitForIdle();ui.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty()}
        runBlocking{assertEquals(200.0,db.cashBoxDao().getById("cash")!!.currentBalance,0.0);val movement=db.cashBoxMovementDao().getAllOnce().single();assertEquals("INITIAL_BALANCE",movement.type);assertEquals(200.0,movement.amountDelta,0.0)}
    }
    @Test fun correctionLedgerFailureRollsBackTheBalanceAndRetryCreatesOneDelta(){
        seed(true);failLedger();screen();ui.onNodeWithContentDescription("تعديل الرصيد").performScrollTo().performClick()
        amount().performTextReplacement("130");ui.onNodeWithText("حفظ").performClick();waitForFailure()
        runBlocking{assertEquals(100.0,db.cashBoxDao().getById("cash")!!.currentBalance,0.0);assertTrue(db.cashBoxMovementDao().getAllOnce().isEmpty())}
        amount().assertTextContains("130");db.openHelper.writableDatabase.execSQL("DROP TRIGGER cash_ui_fault")
        ui.onNodeWithText("حفظ").performClick();ui.waitUntil(5000){ui.waitForIdle();ui.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty()}
        runBlocking{assertEquals(130.0,db.cashBoxDao().getById("cash")!!.currentBalance,0.0);val movement=db.cashBoxMovementDao().getAllOnce().single();assertEquals("MANUAL_ADJUSTMENT",movement.type);assertEquals(30.0,movement.amountDelta,0.0);assertEquals(130.0,movement.balanceAfter,0.0)}
    }
}
