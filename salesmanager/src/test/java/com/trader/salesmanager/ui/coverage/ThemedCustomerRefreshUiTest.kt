package com.trader.salesmanager.ui.coverage

import android.content.Intent
import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.customers.list.*
import com.trader.salesmanager.ui.customers.details.*
import com.trader.salesmanager.ui.debts.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers="w480dp-h900dp")
class ThemedCustomerRefreshUiTest:AppThemeUiFixture() {
    @Test fun customerIdentityAndPhoneRefreshReplaceTheVisibleRowAndClickTarget(){
        val vm=mockk<CustomersViewModel>(relaxed=true)
        val live=MutableStateFlow(CustomersUiState(customers=listOf(Customer(7,"زبون قديم",phone="0501111111"))))
        every{vm.uiState} returns live;every{vm.deleteConfirm} returns MutableStateFlow(null)
        var selected=0L
        showApp{CustomersScreen({},{selected=it},{},viewModel=vm)};dark(true)
        ui.runOnIdle{live.value=CustomersUiState(customers=listOf(Customer(9,"زبون بديل",phone="0502222222",syncStatus=SyncStatus.PENDING)),pendingSyncCount=1)}
        ui.onNodeWithText("زبون قديم").assertDoesNotExist();ui.onNodeWithText("0501111111").assertDoesNotExist()
        ui.onNodeWithText("0502222222").assertExists();dark(false)
        ui.onNodeWithText("زبون بديل").performClick();assertEquals(9L,selected)
        verify(exactly=0){vm.confirmDelete()}
    }
    @Test fun customerSearchAndNoResultStateRemainAfterThemeThenRecoverOnLiveMatch(){
        val vm=mockk<CustomersViewModel>(relaxed=true)
        val live=MutableStateFlow(CustomersUiState(searchQuery="سارة"))
        every{vm.uiState} returns live;every{vm.deleteConfirm} returns MutableStateFlow(null)
        showApp{CustomersScreen({},{},{},viewModel=vm)};dark(true)
        ui.onNode(hasSetTextAction()).assertTextContains("سارة")
        ui.runOnIdle{live.value=live.value.copy(customers=listOf(Customer(1,"سارة مطابقة")))}
        ui.onNodeWithText("سارة مطابقة").assertExists();dark(false)
        ui.onNode(hasSetTextAction()).assertTextContains("سارة")
        verify(exactly=0){vm.updateSearch(any())}
    }
    @Test fun deletionConfirmationUpdatesTheWarningAndNeverConfirmsOnAThemeChange(){
        val vm=mockk<CustomersViewModel>(relaxed=true)
        val customer=Customer(8,"زبون تأكيد")
        val confirm=MutableStateFlow<DeleteConfirmState?>(DeleteConfirmState(customer,0))
        every{vm.uiState} returns MutableStateFlow(CustomersUiState(customers=listOf(customer)))
        every{vm.deleteConfirm} returns confirm
        showApp{CustomersScreen({},{},{},viewModel=vm)};dark(true)
        ui.onNodeWithText("حذف رغم ذلك").assertDoesNotExist()
        ui.runOnIdle{confirm.value=DeleteConfirmState(customer,2)}
        ui.onNodeWithText("يملك 2 عملية مرتبطة").assertExists();dark(false)
        verify(exactly=0){vm.confirmDelete()}
        ui.onNodeWithText("إلغاء").performClick();verify(exactly=1){vm.dismissDelete()}
    }
    @Test fun settledDebtDisappearsAndReorderedRankingNavigatesToTheCurrentCustomer(){
        val vm=mockk<DebtsViewModel>(relaxed=true)
        val live=MutableStateFlow(DebtsUiState(listOf(CustomerDebt(Customer(1,"مدين قديم"),100.0),CustomerDebt(Customer(2,"مدين باق"),50.0)),false))
        every{vm.uiState} returns live;var selected=0L
        showApp{DebtsScreen({},{selected=it},viewModel=vm)};dark(true)
        ui.runOnIdle{live.value=DebtsUiState(listOf(CustomerDebt(Customer(2,"مدين باق"),25.0),CustomerDebt(Customer(3,"مدين جديد"),10.0)),false)}
        ui.onNodeWithText("مدين قديم").assertDoesNotExist();ui.onNodeWithText("مدين جديد").performClick();assertEquals(3L,selected)
        dark(false);ui.runOnIdle{live.value=DebtsUiState(emptyList(),false)}
        ui.onNodeWithText("لا توجد ديون 🎉").assertExists();ui.onNodeWithText("مدين باق").assertDoesNotExist()
    }
    @Test fun customerStatementRefreshReplacesTransactionsAndClearsItsDebtWarning(){
        val vm=mockk<CustomerDetailsViewModel>(relaxed=true)
        val tx=Transaction(40,7,"حركة قديمة",100.0,isPaid=false,paymentMethodName="مرجع قديم")
        val live=MutableStateFlow(CustomerDetailsUiState(Customer(7,"زبون كشف"),listOf(tx),100.0,false))
        every{vm.uiState} returns live;var selected=0L
        showApp{CustomerDetailsScreen(7,{},{},{},{selected=it},vm)}
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("إجمالي الديون"));dark(true)
        ui.runOnIdle{live.value=live.value.copy(transactions=listOf(tx.copy(id=41,isPaid=true,paymentMethodName="مرجع جديد")),totalDebt=0.0)}
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("لا توجد ديون"))
        ui.onNodeWithText("إجمالي الديون").assertDoesNotExist();dark(false)
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("مرجع جديد"))
        ui.onNodeWithText("مرجع قديم").assertDoesNotExist();ui.onNodeWithText("مرجع جديد").performClick();assertEquals(41L,selected)
    }
    @Test fun changedCustomerPhoneUsesTheCurrentDialIntentThenRemovesActionsWhenCleared(){
        val vm=mockk<CustomerDetailsViewModel>(relaxed=true)
        val live=MutableStateFlow(CustomerDetailsUiState(customer=Customer(7,"زبون اتصال",phone="0501111111"),isLoading=false))
        every{vm.uiState} returns live
        showApp{CustomerDetailsScreen(7,{},{},{},{},vm)};dark(true)
        ui.runOnIdle{live.value=live.value.copy(customer=live.value.customer!!.copy(phone="0502222222"))}
        ui.onNodeWithText("اتصال مباشر").performClick()
        val intent=shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_DIAL,intent.action);assertEquals("tel:0502222222",intent.dataString)
        dark(false);ui.runOnIdle{live.value=live.value.copy(customer=live.value.customer!!.copy(phone=""))}
        ui.onNodeWithText("اتصال مباشر").assertDoesNotExist();ui.onNodeWithText("واتساب 972").assertDoesNotExist()
    }
}
