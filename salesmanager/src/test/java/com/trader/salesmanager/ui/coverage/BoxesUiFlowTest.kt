package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.boxes.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BoxesUiFlowTest:OfflineSellerUiTest() {
    private val repo=mockk<CashBoxRepository>(relaxed=true)
    private val methods=mockk<PaymentMethodRepository>(relaxed=true)
    private val boxes=MutableStateFlow(listOf(CashBox("cash",1,"نقد الاختبار",currentBalance=125.0,initialBalance=100.0,initialBalanceSetAt=1,merchantId="fixture"),CashBox("bank",2,"بنك الاختبار",merchantId="fixture")))
    @Before fun data() {
        every { repo.getAllBoxes() } returns boxes
        every { repo.getAllMovements() } returns MutableStateFlow(listOf(CashBoxMovement("m","cash","نقد الاختبار",CashBoxMovementType.TRANSACTION_EFFECT,25.0,125.0,note="بيع الاختبار")))
        every { methods.getAllPaymentMethods() } returns MutableStateFlow(listOf(PaymentMethod(1,"نقد",PaymentType.CASH),PaymentMethod(2,"بنك",PaymentType.BANK)))
    }
    @Test fun initialBalanceDialogPassesArabicAmountToCorrectBox() {
        val vm=owned(BoxesViewModel(repo,methods,activity))
        show { BoxesScreen({},viewModel=vm) }
        ui.onNodeWithText("حدد الرصيد الابتدائي").performScrollTo().performClick()
        ui.onAllNodes(hasSetTextAction())[0].performTextReplacement("٢٠٠")
        ui.onNodeWithText("حفظ").performClick()
        coVerify { repo.setInitialBalance("bank",200.0) }
    }
    @Test fun initializedBalanceAndMovementDetailsCanBeExpanded() {
        val vm=owned(BoxesViewModel(repo,methods,activity))
        show { BoxesScreen({},viewModel=vm) }
        ui.onAllNodesWithText("نقد الاختبار").onFirst().performClick()
        ui.onNodeWithText("حركات هذا الصندوق").assertExists()
        ui.onNodeWithText("الرصيد الإجمالي").assertExists()
        Assert.assertEquals(125.0,vm.uiState.value.totalBalance,0.0)
    }
}
