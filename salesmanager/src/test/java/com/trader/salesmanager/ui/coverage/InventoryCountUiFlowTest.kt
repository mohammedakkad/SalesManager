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
class InventoryCountUiFlowTest:OfflineSellerUiTest() {
    @Test fun enteringPhysicalCountUpdatesOnlyTheSelectedInventoryItem() {
        val repo=mockk<InventoryRepository>(relaxed=true)
        val session=InventorySession(id="s",merchantId="fixture")
        val item=InventorySessionItem("a","s","p","أرز الجرد","u","حبة أرز",5.0)
        val counted=InventorySessionItem("b","s","q","سكر الجرد","v","حبة",4.0,4.0)
        val items=MutableStateFlow(listOf(item,counted))
        every { repo.getActiveSession() } returns MutableStateFlow(session)
        every { repo.getAllSessions() } returns MutableStateFlow(listOf(session))
        every { repo.getSessionItems("s") } returns items
        coEvery { repo.updateSessionItem(any()) } coAnswers { val updated=firstArg<InventorySessionItem>();items.value=items.value.map { if(it.id==updated.id) updated else it } }
        val vm=owned(InventorySessionViewModel(repo,mockk<ProductRepository>(relaxed=true),"fixture"))
        show { InventorySessionScreen({},viewModel=vm) }
        ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("حبة أرز"))
        ui.onNodeWithText("حبة أرز").performClick()
        ui.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement("7")
        ui.onNodeWithText("تأكيد").performClick()
        coVerify { repo.updateSessionItem(item.copy(actualQuantity=7.0)) }
        ui.runOnIdle { Assert.assertEquals(2,vm.countedItems);Assert.assertEquals(1,vm.adjustmentsCount) }
    }
    @Test fun startingCountDoesNotPretendItIsAlreadyFinished() {
        val repo=mockk<InventoryRepository>(relaxed=true)
        every { repo.getActiveSession() } returns MutableStateFlow(null)
        every { repo.getAllSessions() } returns MutableStateFlow(listOf(InventorySession("old","fixture",InventoryStatus.FINISHED,totalAdjustments=2)))
        val vm=owned(InventorySessionViewModel(repo,mockk<ProductRepository>(relaxed=true),"fixture"))
        show { InventorySessionScreen({},viewModel=vm) }
        ui.onNodeWithText("بدء جرد جديد").performClick()
        coVerify { repo.startNewSession("fixture") }
        Assert.assertNull(vm.uiState.value.activeSession)
    }
}
