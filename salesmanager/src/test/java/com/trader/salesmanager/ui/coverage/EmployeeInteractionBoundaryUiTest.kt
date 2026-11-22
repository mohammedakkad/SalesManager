package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.employees.*
import io.mockk.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Seller employee UI only; these role chips do not qualify server authorization. */
@RunWith(RobolectricTestRunner::class)
class EmployeeInteractionBoundaryUiTest:OfflineSellerUiTest() {
    private val vm=mockk<EmployeeManagementViewModel>(relaxed=true)
    private val form=MutableStateFlow(AddEmployeeForm())
    private val employees=MutableStateFlow<List<Employee>>(emptyList())
    private val events=Channel<EmployeeManagementEvent>(Channel.BUFFERED)
    @Before fun model() {
        every { vm.form } returns form;every { vm.employees } returns employees;every { vm.events } returns events.receiveAsFlow()
    }
    @After fun closeEvents() { events.close() }
    private fun render(back:()->Unit={}) { show { EmployeeManagementScreen(back,vm) } }
    private fun openSheet() { ui.onNodeWithText("إضافة موظف",useUnmergedTree=true).performClick();ui.onNodeWithText("إضافة موظف جديد").assertExists() }
    @Test fun emptyEmployeeListExplainsCreationAndBackNavigates() {
        var back=0;render { back++ };ui.onNodeWithText("لا يوجد موظفون بعد").assertExists()
        ui.onNodeWithContentDescription("رجوع").performClick();assertEquals(1,back)
    }
    @Test fun blankLegacyEmployeeNameUsesAFallbackAvatarWithoutDroppingTheRecord() {
        employees.value=listOf(Employee(id="legacy",name="   ",pinCode="1234"));render()
        ui.onNodeWithText("?").assertExists();ui.onNodeWithContentDescription("حذف").assertExists()
        ui.onNodeWithText("لا يوجد موظفون بعد").assertDoesNotExist()
    }
    @Test fun saveGatesFollowBothNameAndPinAcrossModelUpdates() {
        render();openSheet();ui.onNodeWithText("حفظ").assertIsNotEnabled()
        ui.runOnIdle { form.value=AddEmployeeForm("س","1234") };ui.onNodeWithText("حفظ").assertIsNotEnabled()
        ui.runOnIdle { form.value=AddEmployeeForm("سارة","123") };ui.onNodeWithText("حفظ").assertIsNotEnabled()
        ui.runOnIdle { form.value=AddEmployeeForm("سارة","1234") };ui.onNodeWithText("حفظ").assertIsEnabled()
        ui.runOnIdle { form.value=form.value.copy(name="  ") };ui.onNodeWithText("حفظ").assertIsNotEnabled()
        verify(exactly=0) { vm.submit() }
    }
    @Test fun distinctValidationErrorsReplaceTheirPredecessorAndClearOnRecovery() {
        render();openSheet()
        val cases=listOf(AddEmployeeError.NAME_BLANK to "الاسم قصير جداً",AddEmployeeError.PIN_INVALID to "رمز PIN يجب أن يكون 4 أرقام",AddEmployeeError.PIN_TAKEN to "هذا الرمز مستخدم بالفعل")
        for((error,text) in cases) {
            ui.runOnIdle { form.value=form.value.copy(error=error) };ui.waitForIdle();ui.onNodeWithText(text).assertExists()
            cases.filter{it.first!=error}.forEach{ui.onNodeWithText(it.second).assertDoesNotExist()}
        }
        ui.runOnIdle { form.value=form.value.copy(error=AddEmployeeError.NONE) };ui.waitForIdle()
        cases.forEach { ui.onNodeWithText(it.second).assertDoesNotExist() }
    }
    @Test fun cancellingSheetResetsTheFormWithoutSubmitting() {
        every { vm.resetForm() } answers { form.value=AddEmployeeForm() }
        form.value=AddEmployeeForm("fixture name","1234");render();openSheet()
        ui.onNodeWithText("إلغاء").performClick();ui.waitForIdle()
        verify(exactly=1) { vm.resetForm() };verify(exactly=0) { vm.submit() }
        ui.onNodeWithText("إضافة موظف جديد").assertDoesNotExist()
        openSheet();ui.onNodeWithText("حفظ").assertIsNotEnabled();ui.onNodeWithText("fixture name").assertDoesNotExist()
    }
    @Test fun roleChipsReflectModelSelectionAndDelegateBothRoles() {
        every { vm.onRoleChange(any()) } answers { form.value=form.value.copy(role=firstArg()) }
        render();openSheet();ui.onNodeWithText("كاشير").assertIsSelected()
        ui.onNodeWithText("مدير").performClick();ui.onNodeWithText("مدير").assertIsSelected();ui.onNodeWithText("كاشير").assertIsNotSelected()
        ui.onNodeWithText("كاشير").performClick();ui.onNodeWithText("كاشير").assertIsSelected()
        verify(exactly=1) { vm.onRoleChange(EmployeeRole.ADMIN) };verify(exactly=1) { vm.onRoleChange(EmployeeRole.CASHIER) }
    }
    @Test fun submissionInProgressShowsADisabledProgressControl() {
        form.value=AddEmployeeForm("سارة","1234",submitting=true);render();openSheet()
        ui.onNodeWithText("حفظ").assertDoesNotExist()
        ui.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).onAncestors().filter(hasClickAction()).onFirst().assertIsNotEnabled()
        verify(exactly=0) { vm.submit() }
    }
    @Test fun addedEventClosesTheSheetAndShowsItsSpecificOutcome() {
        render();openSheet();ui.runOnIdle { events.trySend(EmployeeManagementEvent.EmployeeAdded) };ui.waitForIdle()
        ui.onNodeWithText("إضافة موظف جديد").assertDoesNotExist();ui.onNodeWithText("تمت إضافة الموظف بنجاح").assertExists()
    }
    @Test fun failureEventKeepsTheDraftSheetOpenAndDisplaysTheReason() {
        form.value=AddEmployeeForm("سارة","1234");render();openSheet()
        ui.runOnIdle { events.trySend(EmployeeManagementEvent.Failure("fixture denied")) };ui.waitForIdle()
        ui.onNodeWithText("إضافة موظف جديد").assertExists();ui.onNodeWithText("fixture denied").assertExists()
        verify(exactly=0) { vm.resetForm() }
    }
    @Test fun deletedEventReportsDeletionWithoutStartingAnotherDelete() {
        render();ui.runOnIdle { events.trySend(EmployeeManagementEvent.EmployeeDeleted) };ui.waitForIdle()
        ui.onNodeWithText("تم حذف الموظف").assertExists();verify(exactly=0) { vm.delete(any()) }
    }
}
