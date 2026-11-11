package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.employees.*
import io.mockk.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EmployeeJourneysUiTest:OfflineSellerUiTest() {
    private val vm=mockk<EmployeeManagementViewModel>(relaxed=true)
    private val form=MutableStateFlow(AddEmployeeForm())
    @Before fun fixture() {
        every { vm.employees } returns MutableStateFlow(listOf(Employee(id="admin_fixture",name="مدير المتجر",pinCode="1234",role=EmployeeRole.ADMIN),Employee(id="cashier",name="بائع",pinCode="4321")))
        every { vm.form } returns form;every { vm.events } returns emptyFlow()
    }
    @Test fun employeeSheetForwardsNamePinRoleAndSubmit() {
        show { EmployeeManagementScreen({},vm) }
        ui.onNodeWithText("مدير المتجر").assertExists();ui.onNodeWithText("بائع").assertExists()
        ui.onNodeWithText("إضافة موظف",useUnmergedTree=true).performClick()
        ui.onNodeWithText("اسم الموظف").performTextInput("سارة");verify { vm.onNameChange("سارة") }
        ui.onNodeWithText("رمز PIN (4 أرقام)").performTextInput("6789");verify { vm.onPinChange("6789") }
        ui.onAllNodesWithText("مدير").onLast().performClick();verify { vm.onRoleChange(EmployeeRole.ADMIN) }
        ui.runOnIdle { form.value=AddEmployeeForm("سارة","6789",EmployeeRole.ADMIN) }
        // Submit is the sheet's final button; the FAB shares the add-employee label.
        ui.onNodeWithText("حفظ").performClick();verify { vm.submit() }
    }
    @Test fun deletingCashierRequiresDialogAndCancellationDoesNotDelete() {
        show { EmployeeManagementScreen({},vm) }
        ui.onAllNodesWithContentDescription("حذف").onLast().performClick()
        ui.onNodeWithText("حذف الموظف").assertExists();ui.onNodeWithText("إلغاء").performClick()
        verify(exactly=0) { vm.delete(any()) }
        ui.onAllNodesWithContentDescription("حذف").onLast().performClick()
        ui.onNodeWithText("حذف").performClick();verify { vm.delete(match { it.id=="cashier" }) }
    }
}
