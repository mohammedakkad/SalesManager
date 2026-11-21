package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.CustomerRepository
import com.trader.salesmanager.ui.customers.list.*
import com.trader.salesmanager.ui.customers.addedit.*
import com.trader.salesmanager.ui.debts.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomerJourneysUiTest:OfflineSellerUiTest() {
    @Test fun customerListSearchAndIdentityNavigationAreDelegated() {
        val vm=mockk<CustomersViewModel>(relaxed=true)
        every { vm.uiState } returns MutableStateFlow(CustomersUiState(customers=listOf(Customer(7,"ليلى",phone="0501234567",syncStatus=SyncStatus.PENDING)),pendingSyncCount=1))
        every { vm.deleteConfirm } returns MutableStateFlow(null)
        var selected=0L
        show { CustomersScreen({},{selected=it},{},viewModel=vm) }
        ui.onNodeWithText("ليلى").performClick();assertEquals(7,selected)
        ui.onNode(hasSetTextAction()).performTextInput("لي")
        verify { vm.updateSearch("لي") }
    }
    @Test fun linkedCustomerDeletionRequiresExplicitConfirmation() {
        val customer=Customer(7,"ليلى")
        val vm=mockk<CustomersViewModel>(relaxed=true)
        every { vm.uiState } returns MutableStateFlow(CustomersUiState(customers=listOf(customer)))
        every { vm.deleteConfirm } returns MutableStateFlow(DeleteConfirmState(customer,3))
        show { CustomersScreen({},{},{},viewModel=vm) }
        ui.onNodeWithText("يملك 3 عملية مرتبطة").assertExists()
        verify(exactly=0) { vm.confirmDelete() }
        ui.onNodeWithText("حذف رغم ذلك").performClick();verify(exactly=1) { vm.confirmDelete() }
    }
    @Test fun actualCustomerFormRejectsReservedNameThenSavesTrimmedIdentityOnce() {
        val repo=mockk<CustomerRepository>(relaxed=true);coEvery { repo.getPhoneConflict(any(),any()) } returns null
        val vm=owned(AddEditCustomerViewModel(repo));var backs=0
        show { AddEditCustomerScreen(null,{backs++},vm) }
        ui.onNodeWithText("اسم الزبون *").performTextInput("زبون زائر")
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("إضافة الزبون"))
        ui.onNodeWithText("إضافة الزبون").assertIsNotEnabled()
        ui.onNodeWithText("اسم الزبون *").performTextReplacement("  سامر  ")
        ui.onNodeWithText("إضافة الزبون").performClick()
        ui.waitUntil(3000) { backs==1 }
        coVerify(exactly=1) { repo.insertCustomer(match { it.name=="سامر" }) }
    }
    @Test fun editCustomerFormPreservesLoadedPhoneAndRoutesSave() {
        val vm=mockk<AddEditCustomerViewModel>(relaxed=true)
        every { vm.uiState } returns MutableStateFlow(AddEditCustomerUiState(name="سلمى",phone="0501234567",isEditMode=true))
        show { AddEditCustomerScreen(8,{},vm) }
        verify { vm.loadCustomer(8) }
        ui.onNodeWithText("0501234567").assertExists()
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("حفظ التعديلات"))
        ui.onNodeWithText("حفظ التعديلات").performClick();verify { vm.save() }
    }
    @Test fun debtsNavigateToCorrectCustomerRatherThanDisplayedRank() {
        val vm=mockk<DebtsViewModel>(relaxed=true)
        every { vm.uiState } returns MutableStateFlow(DebtsUiState(listOf(CustomerDebt(Customer(42,"مدين أول"),500.0),CustomerDebt(Customer(71,"مدين ثان"),100.0)),false))
        var selected=0L
        show { DebtsScreen({},{selected=it},viewModel=vm) }
        ui.onNodeWithText("مدين ثان").performClick();assertEquals(71,selected)
        ui.onNodeWithText("2 زبون عليه دين").assertExists()
    }
}
