package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.home.*
import com.trader.salesmanager.ui.customers.list.*
import com.trader.salesmanager.ui.customers.details.*
import com.trader.salesmanager.ui.customers.addedit.*
import com.trader.salesmanager.ui.debts.*
import com.trader.salesmanager.ui.inventory.list.*
import com.trader.salesmanager.ui.inventory.detail.*
import com.trader.salesmanager.ui.inventory.addedit.*
import com.trader.salesmanager.ui.inventory.session.*
import com.trader.salesmanager.ui.inventory.reports.*
import com.trader.salesmanager.ui.transactions.details.*
import com.trader.salesmanager.ui.transactions.list.*
import com.trader.salesmanager.ui.reports.*
import com.trader.salesmanager.ui.returns.*
import com.trader.salesmanager.ui.navigation.AppNavigation
import io.mockk.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

/** Actual NavHost, arguments, destination owners and saved invoice drafts; offline models at repository/UI boundaries. */
@RunWith(RobolectricTestRunner::class)
class NavigationBusinessRoutesUiTest:OfflineNavigationFixture() {
    private val saleLoads=MutableStateFlow<List<Long?>>(emptyList())
    private val selections=MutableStateFlow<List<Long?>>(emptyList())
    private val customerLoads=MutableStateFlow<List<Long?>>(emptyList())
    private val invoiceLoads=MutableStateFlow<List<String>>(emptyList())
    private val productLoaded=MutableStateFlow<String?>(null)
    private val tx=Transaction(42,7,"عميل مسار العملية",50.0,isPaid=false,hasItems=true)
    private val details=mockk<TransactionDetailsViewModel>(relaxed=true)
    private val detailState=MutableStateFlow(TransactionDetailsUiState(transaction=tx,isItemsLoaded=true))
    private val customerDetail=mockk<CustomerDetailsViewModel>(relaxed=true)
    private val customerEditor=mockk<AddEditCustomerViewModel>(relaxed=true)
    private val customerEditorState=MutableStateFlow(AddEditCustomerUiState(name="زبون التعديل",phone="0501234567"))
    private val productDetail=mockk<ProductDetailViewModel>(relaxed=true)
    private val productEditor=mockk<AddEditProductViewModel>(relaxed=true)
    private val productEditorState=MutableStateFlow(AddEditProductUiState(name="صنف المسار"))
    private val productState=MutableStateFlow(ProductDetailUiState())
    private val returns=mockk<ReturnViewModel>(relaxed=true)
    private val returnState=MutableStateFlow(ReturnScreenState(isLoading=false))
    private val reportModel=mockk<ReportsViewModel>(relaxed=true)
    private val customerRequests=mutableListOf<Long>()
    private val transactionRequests=mutableListOf<Long>()
    private val productRequests=mutableListOf<String>()
    private val returnRequests=mutableListOf<Long>()
    private val dayRequests=mutableListOf<Long>()
    private fun product(id:String="p-route")=ProductWithUnits(Product(id,name="صنف المسار"),listOf(ProductUnit("u-route",id,unitLabel="وحدة المسار",price=10.0,quantityInStock=10.0)))
    @Before fun businessModels(){
        val home=mockk<HomeViewModel>(relaxed=true);every{home.uiState} returns MutableStateFlow(HomeUiState(recentTransactions=listOf(tx)))
        val customers=mockk<CustomersViewModel>(relaxed=true);every{customers.uiState} returns MutableStateFlow(CustomersUiState(customers=listOf(customer),isLoading=false));every{customers.deleteConfirm} returns MutableStateFlow(null)
        val debts=mockk<DebtsViewModel>(relaxed=true);every{debts.uiState} returns MutableStateFlow(DebtsUiState(listOf(CustomerDebt(customer,50.0)),false))
        every{stock.uiState} returns MutableStateFlow(InventoryListUiState(products=listOf(product()),isLoading=false))
        every{details.uiState} returns detailState
        every{customerDetail.uiState} returns MutableStateFlow(CustomerDetailsUiState(customer=customer,transactions=listOf(tx),totalDebt=50.0,isLoading=false))
        every{customerEditor.uiState} returns customerEditorState
        every{customerEditor.loadCustomer(any())} answers {customerLoads.value=customerLoads.value+listOf(firstArg<Long?>());Unit}
        every{sale.loadTransaction(any())} answers {saleLoads.value=saleLoads.value+listOf(firstArg<Long?>());Unit}
        every{sale.preselect(any())} answers {selections.value=selections.value+listOf(firstArg<Long?>());Unit}
        every{lineVm.loadExistingLines(any())} answers {invoiceLoads.value=invoiceLoads.value+firstArg<String>();Unit}
        productState.value=ProductDetailUiState(product=product(),isLoading=false)
        every{productDetail.uiState} returns productState;every{productDetail.getMovementsForUnit(any())} returns flowOf(emptyList())
        every{productEditor.uiState} returns productEditorState
        every{productEditor.loadProduct(any())} answers {productLoaded.value=firstArg();Unit}
        every{returns.state} returns returnState
        every{reportModel.uiState} returns MutableStateFlow(ReportsUiState(isLoading=false,calendarMonth=0,calendarYear=2026,selectedDay=3,selectedDayTransactions=listOf(tx),selectedDaySummary=Triple(50.0,0.0,50.0)))
        val day=mockk<DayTransactionsViewModel>(relaxed=true);every{day.uiState} returns MutableStateFlow(DayTxUiState(all=listOf(tx),isLoading=false))
        val sessions=mockk<InventorySessionViewModel>(relaxed=true);every{sessions.uiState} returns MutableStateFlow(InventorySessionUiState(isLoading=false));every{sessions.sessionItems} returns MutableStateFlow(emptyList())
        val stockReports=mockk<StockReportsViewModel>(relaxed=true);every{stockReports.uiState} returns MutableStateFlow(StockReportsUiState(isLoading=false))
        val list=mockk<TransactionsViewModel>(relaxed=true);every{list.uiState} returns MutableStateFlow(TransactionsUiState(transactions=listOf(tx)))
        loadKoinModules(module{
            viewModel{home};viewModel{customers};viewModel{debts};viewModel{reportModel};viewModel{sessions};viewModel{stockReports};viewModel{list}
            viewModel{parameters->transactionRequests+=parameters.get<Long>();details}
            viewModel{parameters->customerRequests+=parameters.get<Long>();customerDetail}
            viewModel{customerEditor};viewModel{productEditor}
            viewModel{parameters->productRequests+=parameters.get<String>();productDetail}
            viewModel{parameters->returnRequests+=parameters.get<Long>();returns}
            viewModel{parameters->dayRequests+=parameters.get<Long>();day}
        })
    }
    private fun start(){show{AppNavigation()}}
    private fun tab(text:String){ui.onNode(hasText(text) and isSelectable()).performClick()}
    private fun invoice(){start();ui.onNodeWithText(tx.customerName).performScrollTo().performClick();ui.waitForIdle();assertEquals(listOf(42L),transactionRequests)}
    private fun customerPage(){start();tab("الزبائن");ui.onNodeWithText(customer.name).performClick();ui.waitForIdle();assertEquals(listOf(7L),customerRequests)}
    @Test fun recentSaleOpensItsActualTransactionIdentity(){invoice();ui.onNodeWithContentDescription("مرتجع").assertExists()}
    @Test fun editingSalePassesItsIdAndDoesNotPreselectAnotherCustomer(){
        saleState.value=saleState.value.copy(isEditMode=true);invoice();ui.onNodeWithContentDescription("تعديل العملية").performClick()
        ui.waitUntil(5000){ui.waitForIdle();saleLoads.value.contains(42L)}
        verify(exactly=1){sale.loadTransaction(42L)};verify(exactly=1){sale.preselect(null)}
    }
    @Test fun returningSaleUsesTheOriginalTransactionAndReturnsToItsDetails(){
        invoice();ui.onNodeWithContentDescription("مرتجع").performClick();ui.waitForIdle();assertEquals(listOf(42L),returnRequests)
        ui.runOnIdle{returnState.value=returnState.value.copy(processingState=ReturnUiState.Success(ReturnInvoice(id="fixture-return",originalTransactionId=42)))}
        ui.waitUntil(5000){ui.waitForIdle();ui.onAllNodesWithContentDescription("تعديل العملية").fetchSemanticsNodes().isNotEmpty()}
        assertEquals(42L,transactionRequests.last())
    }
    @Test fun editingCustomerPreservesTheSelectedIdentityAndReturnsToItsDetails(){
        customerEditorState.value=customerEditorState.value.copy(isEditMode=true);customerPage()
        ui.onNodeWithContentDescription("تعديل الزبون").performClick();ui.waitUntil(5000){ui.waitForIdle();customerLoads.value.contains(7L)};verify(exactly=1){customerEditor.loadCustomer(7L)}
        ui.runOnIdle{customerEditorState.value=customerEditorState.value.copy(isSaved=true)}
        ui.waitUntil(5000){ui.waitForIdle();ui.onAllNodesWithContentDescription("تعديل الزبون").fetchSemanticsNodes().isNotEmpty()}
        assertEquals(7L,customerRequests.last())
    }
    @Test fun customerSalePreselectsTheCustomerFromTheRoute(){
        customerPage();ui.onNodeWithContentDescription("إضافة عملية للزبون").performClick()
        ui.waitUntil(5000){ui.waitForIdle();selections.value.contains(7L)}
        verify(exactly=1){sale.preselect(7L)};verify(exactly=1){sale.loadTransaction(null)}
    }
    @Test fun customerCreationReturnsToTheSameMainTabAfterSuccessfulSave(){
        start();tab("الزبائن");ui.onNodeWithContentDescription("إضافة زبون").performClick()
        ui.onNode(hasSetTextAction() and hasText("اسم الزبون *")).assertExists();ui.waitUntil(5000){ui.waitForIdle();customerLoads.value.contains(null)};verify(exactly=1){customerEditor.loadCustomer(null)}
        ui.runOnIdle{customerEditorState.value=customerEditorState.value.copy(isSaved=true)}
        ui.onNode(hasText("الزبائن") and isSelectable()).assertIsSelected()
    }
    @Test fun debtRowOpensTheSelectedCustomerRatherThanAListPosition(){
        start();tab("الديون");ui.onNodeWithText(customer.name).performClick();ui.waitForIdle();assertEquals(listOf(7L),customerRequests)
    }
    private fun editProduct(id:String){
        val selected=product(id);every{stock.uiState} returns MutableStateFlow(InventoryListUiState(products=listOf(selected),isLoading=false))
        productState.value=ProductDetailUiState(product=selected,isLoading=false);productEditorState.value=productEditorState.value.copy(productId=id,isEditing=true)
        start();tab("المخزن");ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("صنف المسار"));ui.onNodeWithText("صنف المسار").performClick();ui.waitForIdle();assertEquals(listOf(id),productRequests)
        ui.onNodeWithContentDescription("تعديل الصنف").performClick();ui.waitUntil(5000){ui.waitForIdle();productLoaded.value==id};verify(exactly=1){productEditor.loadProduct(id)}
    }
    @Test fun productDetailsAndEditorUseTheSamePersistedId()=editProduct("p-route")
    @Test fun productRoutesPreserveLiteralPlusPercentQueryAndFragmentCharacters()=editProduct("sku+50% ?#tail")
    @Test fun inventorySessionDestinationCanReturnToTheInventoryTab(){
        start();tab("المخزن");ui.onNodeWithContentDescription("جرد المخزون").performClick();ui.onNodeWithText("بدء جرد جديد").assertExists()
        ui.onNodeWithContentDescription("رجوع").performClick();ui.onNode(hasText("المخزن") and isSelectable()).assertIsSelected()
    }
    @Test fun inventoryReportsDestinationCanReturnToTheInventoryTab(){
        start();tab("المخزن");ui.onNodeWithContentDescription("تقارير المخزن").performClick();ui.onNodeWithText("تقارير المخزن والكاش").assertExists()
        ui.onNodeWithContentDescription("رجوع").performClick();ui.onNode(hasText("المخزن") and isSelectable()).assertIsSelected()
    }
    @Test fun reportDayPreservesMidnightAndCanOpenItsActualInvoice(){
        start();tab("التقارير");ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("عرض كل العمليات (1)"))
        ui.onNodeWithText("عرض كل العمليات (1)").performScrollTo().performClick();ui.waitForIdle()
        val expected=java.util.Calendar.getInstance().apply{clear();set(2026,0,3)}.timeInMillis
        assertEquals(listOf(expected),dayRequests)
        ui.onNodeWithText(tx.customerName).performScrollTo().performClick();ui.waitForIdle();assertEquals(42L,transactionRequests.last())
    }
    @Test fun editedInvoiceDraftRoundTripsOnceThroughTheEditorSavedState(){
        saleState.value=saleState.value.copy(isEditMode=true);val existing="[{\"productId\":\"p\",\"unitId\":\"u\",\"displayQty\":2,\"displayWeightUnit\":\"POUND\",\"price\":45}]"
        every{sale.serializePendingLines()} returns existing
        invoice();ui.onNodeWithContentDescription("تعديل العملية").performClick()
        ui.onNodeWithText("إضافة أصناف من المخزن (اختياري)").performClick();ui.waitUntil(5000){ui.waitForIdle();invoiceLoads.value.contains(existing)};verify(exactly=1){lineVm.loadExistingLines(existing)}
        ui.onNode(hasText("تأكيد",substring=true) and hasClickAction()).performClick()
        ui.waitUntil(10000){ui.waitForIdle();appliedJson!=null}
        verify(exactly=1){sale.applyInvoiceLinesFromJson(any())};assertEquals("u",JSONArray(appliedJson).getJSONObject(0).getString("unitId"))
    }
    @Test fun deletionResultPopsDetailsOnlyAfterTheModelConfirmsDeletion(){
        invoice();ui.onNodeWithText("مدير المبيعات").assertDoesNotExist()
        ui.runOnIdle{detailState.value=detailState.value.copy(isDeleted=true)}
        ui.onNodeWithText("مدير المبيعات").assertExists();assertEquals(listOf(42L),transactionRequests)
    }
    @Test fun addingAProductWithoutScanningDoesNotInventAnInitialBarcode(){
        start();tab("المخزن");ui.onNodeWithContentDescription("إضافة صنف").performClick()
        ui.onNode(hasSetTextAction() and hasText("اسم الصنف *")).assertExists()
        verify(exactly=0){productEditor.initWithBarcode(any())};verify(exactly=0){productEditor.loadProduct(any())}
    }
}
