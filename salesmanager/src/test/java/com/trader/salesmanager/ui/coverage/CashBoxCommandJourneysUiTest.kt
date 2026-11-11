package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.boxes.*
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class CashBoxCommandJourneysUiTest:OfflineSellerUiTest() {
    private val repo=mockk<CashBoxRepository>(relaxed=true)
    private val methods=mockk<PaymentMethodRepository>(relaxed=true)
    private val box=CashBox("cash",1,"صندوق الاختبار",currentBalance=100.0,initialBalance=100.0,initialBalanceSetAt=1,merchantId="m")
    private val boxes=MutableStateFlow(listOf(box))
    private val movements=MutableStateFlow(emptyList<CashBoxMovement>())
    private val releases=mutableListOf<CompletableDeferred<Unit>>()
    private lateinit var vm:BoxesViewModel
    @Before fun setup(){
        ShadowToast.reset();every{repo.getAllBoxes()} returns boxes;every{repo.getAllMovements()} returns movements
        every{methods.getAllPaymentMethods()} returns MutableStateFlow(listOf(PaymentMethod(1,"نقد",PaymentType.CASH)))
        vm=owned(BoxesViewModel(repo,methods,activity))
    }
    @After fun cleanup(){releases.forEach{it.complete(Unit)}}
    private fun screen(){show{BoxesScreen({},vm)}}
    private fun initial(){boxes.value=listOf(box.copy(currentBalance=0.0,initialBalance=0.0,initialBalanceSetAt=null));screen();ui.onNodeWithText("حدد الرصيد الابتدائي").performScrollTo().performClick()}
    private fun adjust(){screen();ui.onNodeWithContentDescription("تعديل الرصيد").performScrollTo().performClick()}
    private fun amount()=ui.onNode(hasSetTextAction() and hasText("المبلغ") and hasAnyAncestor(isDialog()))
    private fun chooseReason(label:String){ui.onNode(hasText(label) and hasClickAction()).performScrollTo().assertIsDisplayed().performClick().assertIsSelected()}
    private fun note()=ui.onNode(hasSetTextAction() and hasText("ملاحظة (اختياري)") and hasAnyAncestor(isDialog()))
    @Test fun invalidInitialAmountRemainsEditableInsteadOfDiscardingTheDialog(){
        initial();amount().performTextReplacement("Infinity");ui.onNodeWithText("حفظ").performClick()
        ui.onNodeWithText("تحديد الرصيد الابتدائي").assertExists();amount().assertTextContains("Infinity")
        assertEquals("أدخل مبلغاً صحيحاً",ShadowToast.getTextOfLatestToast());coVerify(exactly=0){repo.setInitialBalance(any(),any())}
    }
    @Test fun initialFailureKeepsArabicAmountAndCanBeRetriedExplicitly(){
        coEvery{repo.setInitialBalance("cash",200.0)} throws IOException("فشل محلي") andThen Unit
        initial();amount().performTextReplacement("٢٠٠");ui.onNodeWithText("حفظ").performClick()
        ui.waitUntil(5000){ui.waitForIdle();ShadowToast.getTextOfLatestToast()?.contains("فشل محلي")==true}
        amount().assertTextContains("٢٠٠");ui.onNodeWithText("حفظ").performClick()
        ui.waitUntil(5000){ui.waitForIdle();ui.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty()}
        coVerify(exactly=2){repo.setInitialBalance("cash",200.0)}
    }
    @Test fun adjustmentFailureRetainsTheAmountNoteAndReasonForRetry(){
        coEvery{repo.adjustBalance(any(),any(),any(),any())} throws IOException("فشل التسوية") andThen Unit
        adjust();amount().performTextReplacement("130");note().performTextReplacement("  فرق الجرد  ");chooseReason("جرد");ui.onNodeWithText("حفظ").performClick()
        ui.waitUntil(5000){ui.waitForIdle();ShadowToast.getTextOfLatestToast()?.contains("فشل التسوية")==true}
        amount().assertTextContains("130");note().assertTextContains("  فرق الجرد  ")
        ui.onNodeWithText("جرد").assertIsSelected();ui.onNodeWithText("حفظ").performClick()
        ui.waitUntil(5000){ui.waitForIdle();ui.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty()}
        coVerify(exactly=2){repo.adjustBalance("cash",130.0,"جرد: فرق الجرد",AdjustmentReason.COUNT)}
    }
    @Test fun pendingInitialSaveKeepsTheDialogAndBlocksSaveAndDismissUntilAcknowledged(){
        val gate=CompletableDeferred<Unit>().also{releases+=it};coEvery{repo.setInitialBalance(any(),any())} coAnswers{gate.await()}
        initial();amount().performTextReplacement("50");ui.onNodeWithText("حفظ").performClick()
        ui.onNodeWithText("حفظ").assertIsNotEnabled();ui.onNodeWithText("إلغاء").assertIsNotEnabled()
        ui.onAllNodes(hasSetTextAction() and hasAnyAncestor(isDialog())).assertCountEquals(0);coVerify(exactly=1){repo.setInitialBalance("cash",50.0)}
        ui.runOnIdle{gate.complete(Unit)};ui.waitUntil(5000){ui.waitForIdle();ui.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty()}
    }
    @Test fun duplicateAdjustmentCannotQueueASecondRepositoryCommand(){
        val gate=CompletableDeferred<Unit>().also{releases+=it};coEvery{repo.adjustBalance(any(),any(),any(),any())} coAnswers{gate.await()}
        screen();ui.runOnIdle{vm.adjustBalance("cash","130",null,AdjustmentReason.CORRECTION);vm.adjustBalance("cash","130",null,AdjustmentReason.CORRECTION)}
        ui.waitForIdle();coVerify(exactly=1){repo.adjustBalance("cash",130.0,"تصحيح",AdjustmentReason.CORRECTION)}
    }
    @Test fun cancelledAdjustmentRetainsTheDraftAndAllowsAnotherAttempt(){
        coEvery{repo.adjustBalance(any(),any(),any(),any())} throws kotlinx.coroutines.CancellationException("fixture cancellation")
        adjust();amount().performTextReplacement("75");ui.onNodeWithText("حفظ").performClick();ui.waitForIdle()
        amount().assertTextContains("75");ui.onNodeWithText("حفظ").assertIsEnabled()
        coVerify(exactly=1){repo.adjustBalance("cash",75.0,"تصحيح",AdjustmentReason.CORRECTION)}
    }
    @Test fun transferReasonSendsAnAbsoluteBalanceAndTrimmedExplanation(){
        adjust();amount().performTextReplacement("١٣٠.٥");note().performTextReplacement("  تحويل موثق  ")
        chooseReason("تحويل");ui.onNodeWithText("حفظ").performClick()
        coVerify(exactly=1){repo.adjustBalance("cash",130.5,"تحويل: تحويل موثق",AdjustmentReason.TRANSFER)}
    }
    @Test fun blankExplanationDoesNotInventAColonOrASecondCashEffect(){
        adjust();amount().performTextReplacement("0");note().performTextReplacement("   ");chooseReason("أخرى");ui.onNodeWithText("حفظ").performClick()
        coVerify(exactly=1){repo.adjustBalance("cash",0.0,"أخرى",AdjustmentReason.OTHER)}
    }
    @Test fun cancellingAnEditedDraftDoesNotCallTheRepository(){
        adjust();amount().performTextReplacement("999");note().performTextReplacement("لن تحفظ")
        ui.onNodeWithText("إلغاء").performClick();coVerify(exactly=0){repo.adjustBalance(any(),any(),any(),any())}
    }
    @Test fun accessibleBackActionNavigatesExactlyOnce(){
        var back=0;show{BoxesScreen({back++},vm)};ui.onNodeWithContentDescription("رجوع").performClick();assertEquals(1,back)
    }
    @Test fun ledgerRetainsSignedAmountsKindsNotesAndRelativeTimeBuckets(){
        val now=System.currentTimeMillis()
        movements.value=listOf(
            CashBoxMovement("a","cash",box.paymentMethodName,CashBoxMovementType.INITIAL_BALANCE,100.0,100.0,note="رصيد افتتاح",createdAt=now),
            CashBoxMovement("b","cash",box.paymentMethodName,CashBoxMovementType.TRANSACTION_EFFECT,-25.5,74.5,note="رد مبلغ",createdAt=now-120000),
            CashBoxMovement("c","cash",box.paymentMethodName,CashBoxMovementType.MANUAL_ADJUSTMENT,0.0,74.5,note="جرد مطابق",createdAt=now-7200000))
        screen()
        for(label in listOf("رصيد افتتاح","رد مبلغ","جرد مطابق","+100.00","-25.50","+0.00","قبل 2 د","قبل 2 س"))ui.onNodeWithText(label).performScrollTo().assertExists()
    }
    @Test fun anEmptyBoxCollectionShowsAnEmptyStateAndNoAdjustmentActions(){
        boxes.value=emptyList();screen();ui.onNodeWithText("لا توجد صناديق بعد").performScrollTo().assertExists()
        ui.onNodeWithContentDescription("تعديل الرصيد").assertDoesNotExist();assertEquals(0.0,vm.uiState.value.totalBalance,0.0)
    }
}
