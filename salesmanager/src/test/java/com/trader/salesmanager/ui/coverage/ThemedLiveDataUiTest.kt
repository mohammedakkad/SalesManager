package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import com.google.firebase.Timestamp
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.core.util.NetworkMonitor
import com.trader.salesmanager.sales.SaleCommands
import com.trader.salesmanager.ui.transactions.list.*
import com.trader.salesmanager.ui.transactions.details.*
import com.trader.salesmanager.ui.inventory.list.*
import com.trader.salesmanager.ui.chat.*
import com.trader.salesmanager.ui.inventory.reports.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Identity, filtering and privacy checks across real app-theme recompositions and live updates. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers="w480dp-h900dp")
class ThemedLiveDataUiTest:AppThemeUiFixture() {
    private fun checkbox(text:String)=ui.onNode(hasText(text) and SemanticsMatcher.expectValue(SemanticsProperties.Role,Role.Checkbox))
    @Test fun paidInvoiceFilterSurvivesThemeAndAnInvoiceChangingPaymentStatus(){
        val first=Transaction(1,1,"مدفوع أول",50.0,isPaid=true)
        val second=Transaction(2,2,"معلق ثان",75.0,isPaid=false)
        val rows=MutableStateFlow(listOf(first,second));val repo=mockk<TransactionRepository>(relaxed=true)
        every{repo.getAllTransactions()} returns rows;every{repo.getUnpaidTransactions()} returns MutableStateFlow(listOf(second))
        val vm=owned(TransactionsViewModel(repo,mockk<SaleCommands>(),mockk<ActivationRepository>()));var selected=0L
        showApp{TransactionsScreen({}, {selected=it},{},viewModel=vm)}
        checkbox("مدفوع").performClick();dark(true)
        ui.onNodeWithText("مدفوع أول").assertExists();ui.onNodeWithText("معلق ثان").assertDoesNotExist()
        ui.runOnIdle{rows.value=listOf(first.copy(isPaid=false),second.copy(isPaid=true))}
        ui.onNodeWithText("مدفوع أول").assertDoesNotExist();ui.onNodeWithText("معلق ثان").performClick()
        assertEquals(2L,selected);coVerify(exactly=0){repo.updateTransaction(any())}
    }
    @Test fun inventoryLowStockSelectionSurvivesThemeWhileLiveQuantitiesChangeMembership(){
        fun product(id:String,name:String,qty:Double)=ProductWithUnits(Product(id=id,name=name),listOf(ProductUnit(id="u$id",productId=id,quantityInStock=qty,price=25.0,lowStockThreshold=2.0,isDefault=true)))
        val first=product("a","قليل أول",1.0);val second=product("b","متوفر ثان",10.0)
        val rows=MutableStateFlow(listOf(first,second));val repo=mockk<ProductRepository>(relaxed=true);val network=mockk<NetworkMonitor>()
        every{repo.getAllProducts()} returns rows;every{network.isOnlineFlow} returns MutableStateFlow(false)
        val vm=owned(InventoryListViewModel(repo,network));var selected=""
        showApp{InventoryListScreen({}, {selected=it},{},{},viewModel=vm)}
        checkbox("نقص").performClick();dark(true)
        ui.onNodeWithText("قليل أول").assertExists();ui.onNodeWithText("متوفر ثان").assertDoesNotExist()
        ui.runOnIdle{rows.value=listOf(product("a","قليل أول",10.0),product("b","متوفر ثان",1.0))}
        ui.onNodeWithText("قليل أول").assertDoesNotExist();ui.onNodeWithText("متوفر ثان").performClick()
        assertEquals("b",selected);assertEquals(StockFilter.LOW,vm.uiState.value.filter)
    }
    @Test fun invoiceDeletionNavigatesOnceEvenWhenThemeChangesBeforeAndAfterCompletion(){
        val vm=mockk<TransactionDetailsViewModel>(relaxed=true)
        val state=MutableStateFlow(TransactionDetailsUiState(transaction=Transaction(31,1,amount=25.0,isPaid=true),isItemsLoaded=true))
        every{vm.uiState} returns state;var back=0
        showApp{TransactionDetailsScreen(31,{back++},{},{},viewModel=vm)}
        dark(true);assertEquals(0,back)
        ui.runOnIdle{state.value=state.value.copy(isDeleted=true)};ui.runOnIdle{assertEquals(1,back)}
        dark(false);ui.runOnIdle{assertEquals(1,back)}
        verify(exactly=0){vm.delete()}
    }
    @Test fun selectedChatMessageKeepsItsIdentityAfterThemeChangeAndLiveReordering(){
        val vm=mockk<ChatViewModel>(relaxed=true)
        val selected=ChatMessage("selected","رسالة محددة","tenant")
        val other=ChatMessage("other","رسالة أخرى","admin")
        val state=MutableStateFlow(ChatUiState(merchantId="tenant",isLoading=false,messages=listOf(selected,other),selectedIds=setOf("selected"),isSelectionMode=true))
        every{vm.uiState} returns state
        showApp{ChatScreen({},vm)};dark(true)
        ui.runOnIdle{state.value=state.value.copy(messages=listOf(other,selected))}
        ui.onNodeWithText("رسالة محددة").performTouchInput{click()}
        verify(exactly=1){vm.onTap(match{it.id=="selected"})};verify(exactly=0){vm.onTap(match{it.id=="other"})}
        verify(exactly=0){vm.requestDeleteSelected()}
    }
    @Test fun chatDeletionAfterThemeChangeRemovesTheOriginalTextFromTheUi(){
        val vm=mockk<ChatViewModel>(relaxed=true)
        val msg=ChatMessage("message","نص يجب إخفاؤه","admin")
        val state=MutableStateFlow(ChatUiState(merchantId="tenant",isLoading=false,messages=listOf(msg)))
        every{vm.uiState} returns state
        showApp{ChatScreen({},vm)};ui.onNodeWithText(msg.text).assertExists();dark(true)
        ui.runOnIdle{state.value=state.value.copy(messages=listOf(msg.copy(deletedAt=Timestamp(1,0))))}
        ui.onNodeWithText(msg.text).assertDoesNotExist();ui.onNodeWithText("تم حذف هذه الرسالة").assertExists()
        dark(false);ui.onNodeWithText(msg.text).assertDoesNotExist()
    }
    @Test fun stockReportSelectedCashTabSurvivesThemeAndRefreshesLiveTotals(){
        val vm=mockk<StockReportsViewModel>(relaxed=true)
        val state=MutableStateFlow(StockReportsUiState(cashSummary=CashSummary(100.0,50.0,25.0,75.0,100.0),isLoading=false))
        every{vm.uiState} returns state
        showApp{StockReportsScreen({},viewModel=vm)}
        ui.onNodeWithText("الكاش").performClick();dark(true)
        ui.onNodeWithText("إجمالي الكاش المحصّل").assertExists()
        ui.runOnIdle{state.value=state.value.copy(cashSummary=CashSummary(200.0,50.0,25.0,175.0,200.0))}
        dark(false);ui.onNodeWithText("إجمالي الكاش المحصّل").assertExists()
        ui.onAllNodesWithText("200.00",substring=true).onFirst().assertExists()

    }
}
