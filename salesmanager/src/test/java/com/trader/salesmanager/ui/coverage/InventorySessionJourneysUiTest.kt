package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.inventory.session.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InventorySessionJourneysUiTest:OfflineSellerUiTest() {
    private val vm=mockk<InventorySessionViewModel>(relaxed=true)
    private val counted=InventorySessionItem("i","s","p","Rice","u","كيس الاختبار",5.0,5.0)
    private val pending=InventorySessionItem("j","s","q","Sugar","v","علبة الاختبار",4.0)
    private val state=MutableStateFlow(InventorySessionUiState(activeSession=InventorySession("s","m"),isLoading=false))
    private val items=MutableStateFlow(listOf(counted,pending))
    @Before fun setup(){
        every{vm.uiState} returns state;every{vm.sessionItems} returns items
        every{vm.totalItems} answers{items.value.size};every{vm.countedItems} answers{items.value.count{it.actualQuantity!=null}}
        every{vm.adjustmentsCount} returns 0;every{vm.progressPercent} answers{vm.countedItems.toFloat()/vm.totalItems.coerceAtLeast(1)}
    }
    private fun screen(){show{InventorySessionScreen({},vm)}}
    @Test fun emptySearchAmongPendingItemsCannotClaimThatEveryItemWasCounted(){
        state.value=state.value.copy(showOnlyPending=true,searchQuery="Missing");screen()
        ui.onNodeWithText("جميع الأصناف تم عدّها ✅").assertDoesNotExist()
        ui.onNodeWithText("لا توجد أصناف مطابقة للبحث").assertExists()
        assertEquals(1,vm.countedItems);assertEquals(2,vm.totalItems)
    }
    @Test fun allCountedMessageRequiresNoPendingItemsInTheEntireSession(){
        items.value=listOf(counted,pending.copy(actualQuantity=4.0));state.value=state.value.copy(showOnlyPending=true);screen()
        ui.onNodeWithText("جميع الأصناف تم عدّها ✅").assertExists();ui.onNodeWithText("علبة الاختبار").assertDoesNotExist()
    }
    @Test fun searchIsCaseInsensitiveAndPendingFilterStillExcludesCountedItems(){
        state.value=state.value.copy(searchQuery="sUgAr",showOnlyPending=true);screen()
        ui.onNodeWithText("علبة الاختبار").assertExists();ui.onNodeWithText("كيس الاختبار").assertDoesNotExist()
    }
    @Test fun headerBackActionHasAnAccessibleNameAndNavigatesOnce(){
        var backs=0;show{InventorySessionScreen({backs++},vm)}
        ui.onNodeWithContentDescription("رجوع").performClick();assertEquals(1,backs)
    }
    @Test fun cancellingRequiresAnExplicitConfirmationAndCanBeDismissed(){
        screen();ui.onNodeWithContentDescription("إلغاء الجرد").performClick()
        ui.onNodeWithText("سيتم إلغاء جلسة الجرد الحالية دون تطبيق أي تغييرات.").assertExists()
        ui.onNodeWithText("رجوع").performClick();verify(exactly=0){vm.cancelSession()}
        ui.onNodeWithContentDescription("إلغاء الجرد").performClick()
        ui.onNode(hasText("إلغاء الجرد") and hasClickAction()).performClick();verify(exactly=1){vm.cancelSession()}
    }
    @Test fun clearSearchAndPendingFilterActionsAreAccessibleAndDispatchTheirOwnChanges(){
        state.value=state.value.copy(searchQuery="Rice");screen()
        ui.onNodeWithContentDescription("مسح البحث").performClick();verify(exactly=1){vm.setSearch("")}
        ui.onNodeWithContentDescription("عرض غير المعدود فقط").performClick();verify(exactly=1){vm.togglePendingOnly()}
    }
    @Test fun fullyCountedFinishCanBeDismissedWithoutApplyingInventoryChanges(){
        items.value=listOf(counted,pending.copy(actualQuantity=4.0));screen()
        ui.onNodeWithText("إنهاء الجرد وتطبيق التعديلات").performClick()
        ui.onNodeWithText("⚠️ الأصناف غير المعدودة لن يتم تعديل كمياتها.").assertDoesNotExist()
        ui.onNodeWithText("إلغاء").performClick();verify(exactly=0){vm.finishSession()}
        ui.onNodeWithText("إنهاء الجرد وتطبيق التعديلات").assertIsEnabled()
    }
    @Test fun physicalZeroIsAValidCountAndUpdatesOnlyTheSelectedItem(){
        screen();ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("علبة الاختبار"));ui.onNodeWithText("علبة الاختبار").performClick()
        ui.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement("0")
        ui.onNodeWithText("-4").assertExists();ui.onNodeWithText("تأكيد").assertIsEnabled().performClick()
        verify(exactly=1){vm.updateItemCount(pending,0.0)};verify(exactly=0){vm.updateItemCount(counted,any())}
    }
    @Test fun invalidPhysicalCountsCannotSubmitAndUncountedItemsCannotClearAnAbsentCount(){
        screen();ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("علبة الاختبار"));ui.onNodeWithText("علبة الاختبار").performClick()
        ui.onNodeWithText("مسح العدّ").assertDoesNotExist()
        for(value in listOf("bad","NaN","Infinity","-1")){
            ui.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement(value)
            ui.onNodeWithText("تأكيد").assertIsNotEnabled()
        }
        verify(exactly=0){vm.updateItemCount(any(),any())}
        ui.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement("4")
        ui.onNodeWithText("لا يوجد فرق ✓").assertExists();ui.onNodeWithText("تأكيد").assertIsEnabled()
    }
}
