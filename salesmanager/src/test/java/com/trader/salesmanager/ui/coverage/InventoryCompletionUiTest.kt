package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.inventory.session.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InventoryCompletionUiTest:OfflineSellerUiTest() {
    private val vm=mockk<InventorySessionViewModel>(relaxed=true)
    private val items=listOf(InventorySessionItem("i","s","p","أرز","u","كيس",5.0,3.0),InventorySessionItem("j","s","q","سكر","v","علبة",4.0))
    @Before fun fixture(){
        every{vm.uiState} returns MutableStateFlow(InventorySessionUiState(activeSession=InventorySession("s","m1"),isLoading=false))
        every{vm.sessionItems} returns MutableStateFlow(items);every{vm.countedItems} returns 1;every{vm.totalItems} returns 2;every{vm.adjustmentsCount} returns 1;every{vm.progressPercent} returns 0.5f
    }
    @Test fun finishingWarnsAboutUncountedItemsAndRequiresExplicitApply() {
        show{InventorySessionScreen({},vm)}
        ui.onNodeWithText("إنهاء الجرد وتطبيق التعديلات").performClick()
        ui.onNodeWithText("⚠️ الأصناف غير المعدودة لن يتم تعديل كمياتها.").assertExists()
        verify(exactly=0){vm.finishSession()};ui.onNodeWithText("تطبيق وإنهاء").performClick();verify(exactly=1){vm.finishSession()}
    }
    @Test fun countedItemCanBeClearedWithoutChangingOtherItem() {
        show{InventorySessionScreen({},vm)}
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("كيس"));ui.onNodeWithText("كيس").performClick()
        ui.onNodeWithText("مسح العدّ").performClick();verify{vm.clearItemCount(match{it.id=="i"})};verify(exactly=0){vm.clearItemCount(match{it.id=="j"})}
    }
}
