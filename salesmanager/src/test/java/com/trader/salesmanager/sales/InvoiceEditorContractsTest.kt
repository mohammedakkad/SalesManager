package com.trader.salesmanager.sales

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.*
import com.trader.salesmanager.ui.transactions.addedit.AddEditTransactionViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28], manifest=Config.NONE, application=Application::class)
class InvoiceEditorContractsTest {
    private val transactions = mockk<TransactionRepository>()
    private val customers = mockk<CustomerRepository>()
    private val products = mockk<ProductRepository>()
    private val invoices = mockk<InvoiceItemRepository>()
    private val activation = mockk<ActivationRepository>()
    private val commands = mockk<SaleCommands>()
    private val customer = Customer(id=1, name="offline fixture")
    private val unit = ProductUnit(id="unit", productId="product", price=50.0)
    private val product = ProductWithUnits(Product(id="product", name="fixture"), listOf(unit))
    private val sale = Transaction(id=7, customerId=1, amount=110.0, isPaid=true,
        date=11, paidAt=12, dueDate=13, paymentMethodId=3, hasItems=true)
    private val oldLine = InvoiceItem(id="original-line", transactionId=7, productId="product", unitId="unit",
        quantity=2.0, pricePerUnit=50.0, totalPrice=100.0, merchantId="merchant")
    private val created = mutableListOf<AddEditTransactionViewModel>()

    @Before fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { customers.getAllCustomers() } returns flowOf(listOf(customer))
        coEvery { customers.getCustomerById(1) } returns customer
        coEvery { activation.getMerchantCode() } returns "merchant"
        coEvery { transactions.getTransactionById(7) } returns sale
        coEvery { invoices.getItemsForTransactionOnce(7) } returns listOf(oldLine)
        coEvery { products.getProductById("product") } returns product
        coEvery { commands.edit(any(), any(), any(), any(), any()) } returns 7
        coEvery { commands.create(any(), any(), any(), any()) } returns 9
    }
    @After fun teardown() { created.forEach { it.viewModelScope.cancel() }; Dispatchers.resetMain() }
    private fun vm(state: SavedStateHandle = SavedStateHandle()) = AddEditTransactionViewModel(
        transactions, customers, products, mockk(), invoices, activation, commands, state).also { created += it }
    private fun json(qty: Double=3.0, weight: String="KG") = """[{"productId":"product","unitId":"unit","price":50,"displayQty":$qty,"displayWeightUnit":"$weight"}]"""

    @Test fun lateInvoiceLoadKeepsPreviewEqualToCommittedGrossAmount() = runTest {
        val waiting = CompletableDeferred<List<InvoiceItem>>()
        coEvery { invoices.getItemsForTransactionOnce(7) } coAnswers { waiting.await() }
        val vm = vm()
        vm.loadTransaction(7); runCurrent()
        vm.applyInvoiceLinesFromJson(json()); runCurrent()
        assertEquals("150.00", vm.uiState.value.amount)
        waiting.complete(listOf(oldLine)); advanceUntilIdle()
        assertEquals(10.0, vm.uiState.value.baseAmount, 0.0)
        assertEquals("160.00", vm.uiState.value.amount)
        assertEquals(3.0, vm.uiState.value.pendingLines.single().quantity, 0.0)
        vm.save(); advanceUntilIdle()
        coVerify { commands.edit(any(), "merchant", sale, match { it.amount==160.0 }, match { it?.single()?.id=="original-line" }) }
    }

    @Test fun delayedPaymentMethodsResolvePersistedSelectionAndUnchangedEditPreservesItems() = runTest {
        val methods = MutableStateFlow<List<PaymentMethod>>(emptyList())
        val repository = mockk<PaymentMethodRepository>()
        every { repository.getAllPaymentMethods() } returns methods
        val vm = vm(); vm.loadPaymentMethods(repository); vm.loadTransaction(7); advanceUntilIdle()
        assertEquals(3L, vm.uiState.value.pendingPaymentMethodId)
        methods.value = listOf(PaymentMethod(2,"other"), PaymentMethod(3,"original")); runCurrent()
        assertEquals(3L, vm.uiState.value.selectedPaymentMethod?.id)
        assertNull(vm.uiState.value.pendingPaymentMethodId)
        vm.updateNote("changed"); vm.save(); advanceUntilIdle()
        coVerify { commands.edit(any(), "merchant", sale,
            match { it.amount==110.0 && it.date==11L && it.paidAt==12L && it.dueDate==13L && it.paymentMethodId==3L && it.note=="changed" }, null) }
    }

    @Test fun repeatedLoadAndEditedLinesDoNotAccumulateBaseAmountOrReplaceOriginalItemId() = runTest {
        val vm = vm(); vm.loadTransaction(7); advanceUntilIdle()
        vm.applyInvoiceLinesFromJson(json()); advanceUntilIdle()
        vm.loadTransaction(7); advanceUntilIdle()
        assertEquals("160.00", vm.uiState.value.amount)
        vm.applyInvoiceLinesFromJson(json(4.0)); advanceUntilIdle()
        assertEquals("210.00", vm.uiState.value.amount)
        vm.save(); vm.save(); advanceUntilIdle()
        coVerify(exactly=1) { commands.edit(any(), "merchant", sale, match { it.amount==210.0 },
            match { it?.single()?.let { row -> row.id=="original-line" && row.quantity==4.0 && row.totalPrice==200.0 }==true }) }
    }

    @Test fun gramsRoundTripAndPersistAsKilogramsWithStableRetryIdentity() = runTest {
        coEvery { products.getProductById("product") } returns product.copy(units=listOf(unit.copy(unitType=UnitType.WEIGHT)))
        val state = SavedStateHandle()
        val vm = vm(state)
        vm.applyInvoiceLinesFromJson(json(500.0,"GRAM")); advanceUntilIdle()
        assertEquals("25.00", vm.uiState.value.amount)
        val serialized = requireNotNull(vm.serializePendingLines())
        val restored = vm(SavedStateHandle(state.keys().associateWith { state.get<Any?>(it) }))
        restored.applyInvoiceLinesFromJson(serialized); advanceUntilIdle()
        val first = slot<List<InvoiceItem>>(); val second = slot<List<InvoiceItem>>()
        val operations = mutableListOf<String>(); val dates = mutableListOf<Long>()
        coEvery { commands.create(capture(operations), any(), match { dates += it.date; true }, capture(first)) } throws IllegalStateException("retry fixture")
        vm.save(); advanceUntilIdle()
        assertFalse(vm.uiState.value.isSaved)
        val firstId = first.captured.single().id
        assertEquals(0.5, first.captured.single().quantity,0.0)
        assertEquals(25.0, first.captured.single().totalPrice,0.0)
        assertTrue(first.captured.single().unitLabel.contains("غرام"))
        coEvery { commands.create(capture(operations), any(), match { dates += it.date; true }, capture(second)) } returns 9
        restored.save(); advanceUntilIdle()
        assertTrue(restored.uiState.value.isSaved)
        assertEquals(firstId, second.captured.single().id)
        assertEquals(operations.first(), operations.last())
        assertEquals(dates.first(), dates.last())
    }

    @Test fun unrecognizedLegacyWeightUnitUsesExistingKilogramFallback() = runTest {
        coEvery { products.getProductById("product") } returns product.copy(units=listOf(unit.copy(unitType=UnitType.WEIGHT)))
        val vm = vm(); vm.applyInvoiceLinesFromJson(json(2.0,"removed-legacy-unit")); advanceUntilIdle()
        assertEquals(2.0, vm.uiState.value.pendingLines.single().quantity,0.0)
        assertEquals("100.00", vm.uiState.value.amount)
        vm.save(); advanceUntilIdle()
        coVerify { commands.create(any(), "merchant", match { it.amount==100.0 }, match { it.single().quantity==2.0 }) }
    }

    @Test fun arabicAmountCustomerPreselectionAndPaymentChangesReachSaleCommand() = runTest {
        val vm=vm(); vm.preselect(null); vm.preselect(1); advanceUntilIdle()
        vm.updateAmount("۱۲٣"); vm.updateIsPaid(false); vm.updatePaymentType(PaymentType.CASH)
        vm.selectPaymentMethod(PaymentMethod(2,"cash")); vm.updateNote("fixture")
        vm.save(); advanceUntilIdle()
        coVerify { commands.create(any(), "merchant", match { it.customerId==1L && it.amount==123.0 && !it.isPaid && it.paidAt==null && it.paymentType==PaymentType.CASH && it.paymentMethodId==2L && it.note=="fixture" }, emptyList()) }
        assertEquals(9L, vm.uiState.value.savedTransactionId)
        assertNull(vm.serializePendingLines())
    }

    private suspend fun TestScope.rejectDraft(input:String) {
        val vm=vm();vm.loadTransaction(7);advanceUntilIdle()
        val before=vm.uiState.value
        vm.applyInvoiceLinesFromJson(input);advanceUntilIdle()
        val after=vm.uiState.value
        assertNotNull("Invalid or incomplete drafts must be reported",after.error)
        assertEquals(before.pendingLines,after.pendingLines)
        assertEquals(before.amount,after.amount)
        assertEquals(before.hasItems,after.hasItems)
        assertEquals(before.userEditedLines,after.userEditedLines)
        vm.save();advanceUntilIdle()
        coVerify {commands.edit(any(),"merchant",sale,match{it.amount==110.0},null)}
    }
    @Test fun malformedDraftPreservesPreviouslyLoadedInvoiceAndEditIntent()=runTest {rejectDraft("{not-json")}
    @Test fun emptyDraftIsReportedWithoutReplacingTheInvoice()=runTest {rejectDraft("[]")}
    @Test fun missingProductRejectsWholeDraftInsteadOfDroppingOnlyOneLine()=runTest {
        coEvery {products.getProductById("missing")} returns null
        rejectDraft(json().dropLast(1)+",{\"productId\":\"missing\",\"unitId\":\"unit\",\"price\":50,\"displayQty\":1}]")
    }
    @Test fun missingUnitRejectsWholeDraftInsteadOfPartiallyApplyingIt()=runTest {
        rejectDraft(json().dropLast(1)+",{\"productId\":\"product\",\"unitId\":\"missing\",\"price\":50,\"displayQty\":1}]")
    }
    @Test fun olderDraftLookupCannotReplaceANewerSelection()=runTest {
        val vm=vm();vm.loadTransaction(7);advanceUntilIdle()
        lateinit var old:Continuation<ProductWithUnits?>
        var lookups=0
        coEvery {products.getProductById("product")} coAnswers {
            if(lookups++==0) suspendCoroutine {old=it} else product
        }
        vm.applyInvoiceLinesFromJson(json(3.0));runCurrent()
        vm.applyInvoiceLinesFromJson(json(4.0));runCurrent()
        assertEquals("210.00",vm.uiState.value.amount)
        old.resume(product);runCurrent()
        assertEquals("210.00",vm.uiState.value.amount)
        assertEquals(4.0,vm.uiState.value.pendingLines.single().quantity,0.0)
    }
    @Test fun cancelledDraftLookupDoesNotChangeInvoiceOrReportAFalseFailure()=runTest {
        val vm=vm();vm.loadTransaction(7);advanceUntilIdle()
        val before=vm.uiState.value
        coEvery {products.getProductById("product")} coAnswers {awaitCancellation()}
        vm.applyInvoiceLinesFromJson(json());runCurrent();vm.viewModelScope.cancel();runCurrent()
        assertNull(vm.uiState.value.error)
        assertEquals(before.pendingLines,vm.uiState.value.pendingLines)
        assertEquals(before.userEditedLines,vm.uiState.value.userEditedLines)
        coVerify(exactly=0){commands.create(any(),any(),any(),any());commands.edit(any(),any(),any(),any(),any())}
    }

    @Test fun saveWaitsForTheSelectedDraftInsteadOfCommittingTheOldInvoice()=runTest {
        val vm=vm();vm.loadTransaction(7);advanceUntilIdle()
        val lookup=CompletableDeferred<ProductWithUnits?>()
        coEvery {products.getProductById("product")} coAnswers {lookup.await()}
        vm.applyInvoiceLinesFromJson(json(3.0));runCurrent()
        vm.save();runCurrent()
        coVerify(exactly=0){commands.edit(any(),any(),any(),any(),any())}
        lookup.complete(product);runCurrent();vm.save();advanceUntilIdle()
        coVerify(exactly=1){commands.edit(any(),"merchant",sale,match{it.amount==160.0},match{it?.single()?.quantity==3.0})}
    }

    @Test fun nanPriceDraftCannotReplaceAFiniteInvoice()=runTest {
        rejectDraft(json().replace("\"price\":50","\"price\":\"NaN\""))
    }
    @Test fun nanQuantityDraftCannotReplaceAFiniteInvoice()=runTest {
        rejectDraft(json().replace("\"displayQty\":3.0","\"displayQty\":\"NaN\""))
    }
    @Test fun lineTotalOverflowRejectsTheCompleteDraft()=runTest {
        rejectDraft(json(2.0).replace("\"price\":50","\"price\":1e308"))
    }
    @Test fun weightConversionOverflowRejectsTheCompleteDraft()=runTest {
        coEvery {products.getProductById("product")} returns product.copy(units=listOf(unit.copy(unitType=UnitType.WEIGHT)))
        rejectDraft(json(1e308,"POUND").replace("\"price\":50","\"price\":0"))
    }
    @Test fun finiteLinesWhoseSumOverflowsCannotReplaceAFiniteInvoice()=runTest {
        val row=json(1.0).removePrefix("[").removeSuffix("]").replace("\"price\":50","\"price\":1e308")
        rejectDraft("[$row,$row]")
    }
    @Test fun zeroPriceLineRetainsTheExistingPositiveManualBaseAmount()=runTest {
        val vm=vm();vm.loadTransaction(7);advanceUntilIdle()
        vm.applyInvoiceLinesFromJson(json(1.0).replace("\"price\":50","\"price\":0"));advanceUntilIdle()
        assertNull(vm.uiState.value.error)
        assertEquals("10.00",vm.uiState.value.amount)
        vm.save();advanceUntilIdle()
        coVerify {commands.edit(any(),"merchant",sale,match{it.amount==10.0},match{it?.single()?.let{row->row.pricePerUnit==0.0&&row.totalPrice==0.0&&row.quantity==1.0}==true})}
    }
}
