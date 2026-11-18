package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.employees.*
import com.trader.salesmanager.ui.boxes.*
import io.mockk.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.Channel
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers="w480dp-h900dp")
class ThemedStaffAndBoxesRefreshUiTest:AppThemeUiFixture(){
    private val employeeVm=mockk<EmployeeManagementViewModel>(relaxed=true)
    private val employees=MutableStateFlow(listOf(Employee(id="a",name="بائع قديم",pinCode="1234")))
    private val form=MutableStateFlow(AddEmployeeForm("مسودة موظف","6789"))
    private val events=Channel<EmployeeManagementEvent>(Channel.BUFFERED)
    @Before fun staff(){every{employeeVm.employees} returns employees;every{employeeVm.form} returns form;every{employeeVm.events} returns events.receiveAsFlow()}
    @After fun release(){events.close()}
    private fun sheet(){ui.onNodeWithText("إضافة موظف",useUnmergedTree=true).performClick()}
    @Test fun employeeRemovalAndReplacementUseCurrentIdentityForDeletionAfterThemeChange(){
        showApp{EmployeeManagementScreen({},employeeVm)};dark(true)
        val replacement=Employee(id="b",name="بائع جديد",pinCode="4321")
        ui.runOnIdle{employees.value=listOf(replacement)}
        ui.onNodeWithText("بائع قديم").assertDoesNotExist();ui.onNodeWithText("بائع جديد").assertExists()
        dark(false);ui.onNodeWithContentDescription("حذف").performClick()
        verify(exactly=0){employeeVm.delete(any())}
        ui.onNodeWithText("حذف").performClick();verify(exactly=1){employeeVm.delete(replacement)}
    }
    @Test fun employeeDraftAndSelectedRoleSurviveThemeWhileValidationFailureIsReplaced(){
        form.value=form.value.copy(role=EmployeeRole.ADMIN)
        showApp{EmployeeManagementScreen({},employeeVm)};sheet();dark(true)
        ui.onNodeWithText("مسودة موظف").assertExists();ui.onNodeWithText("مدير").assertIsSelected()
        ui.runOnIdle{form.value=form.value.copy(error=AddEmployeeError.PIN_TAKEN)}
        ui.onNodeWithText("هذا الرمز مستخدم بالفعل").assertExists();dark(false)
        ui.runOnIdle{form.value=form.value.copy(pin="7890",error=AddEmployeeError.NONE)}
        ui.onNodeWithText("هذا الرمز مستخدم بالفعل").assertDoesNotExist()
        verify(exactly=0){employeeVm.submit()};verify(exactly=0){employeeVm.resetForm()}
        ui.onNodeWithText("حفظ").performClick();verify(exactly=1){employeeVm.submit()}
    }
    @Test fun delayedEmployeeAddedEventClosesTheThemedSheetWithoutResubmitting(){
        showApp{EmployeeManagementScreen({},employeeVm)};sheet();dark(true)
        ui.runOnIdle{form.value=form.value.copy(submitting=true)}
        ui.onNodeWithText("حفظ").assertDoesNotExist();dark(false)
        ui.runOnIdle{events.trySend(EmployeeManagementEvent.EmployeeAdded)}
        ui.onNodeWithText("إضافة موظف جديد").assertDoesNotExist()
        ui.onNodeWithText("تمت إضافة الموظف بنجاح").assertExists()
        verify(exactly=0){employeeVm.submit()}
    }
    private val cash=CashBox("cash",1,"صندوق حي",currentBalance=100.0,initialBalance=100.0,initialBalanceSetAt=1,merchantId="fixture")
    private fun movement(id:String,box:String,note:String)=CashBoxMovement(id,box,"صندوق حي",CashBoxMovementType.MANUAL_ADJUSTMENT,10.0,110.0,note=note)
    @Test fun expandedBoxLedgerRefreshKeepsOtherBoxesMovementsOutOfItsDetails(){
        val vm=mockk<BoxesViewModel>(relaxed=true)
        val live=MutableStateFlow(BoxesUiState(boxes=listOf(cash),isLoading=false,expandedBoxId="cash"))
        every{vm.uiState} returns live
        showApp{BoxesScreen({},vm)};dark(true)
        ui.runOnIdle{live.value=live.value.copy(movements=listOf(movement("a","cash","حركة الصندوق"),movement("b","other","حركة صندوق آخر")))}
        // One occurrence in the expanded ledger and one in the global ledger.
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("حركة الصندوق"))
        ui.onAllNodes(hasAnyDescendant(hasContentDescription("تعديل الرصيد")) and hasAnyDescendant(hasText("حركة الصندوق"))).onLast().assert(!hasAnyDescendant(hasText("حركة صندوق آخر")))
        dark(false);ui.runOnIdle{live.value=live.value.copy(movements=emptyList())}
        ui.onAllNodesWithText("حركة الصندوق").assertCountEquals(0)
        assertTrue(live.value.movementsForBox("cash").isEmpty())
    }
    @Test fun removingAllBoxesRemovesTheirAdjustmentControlsAcrossThemeChanges(){
        val vm=mockk<BoxesViewModel>(relaxed=true)
        val live=MutableStateFlow(BoxesUiState(boxes=listOf(cash),isLoading=false))
        every{vm.uiState} returns live
        showApp{BoxesScreen({},vm)}
        ui.onNodeWithContentDescription("تعديل الرصيد").performScrollTo().assertExists();dark(true)
        ui.runOnIdle{live.value=live.value.copy(boxes=emptyList())}
        ui.onNodeWithText("لا توجد صناديق بعد").performScrollTo().assertExists()
        ui.onNodeWithContentDescription("تعديل الرصيد").assertDoesNotExist();dark(false)
        verify(exactly=0){vm.adjustBalance(any(),any(),any(),any(),any())}
    }
    @Test fun initializingABoxExternallyReplacesTheInitialActionWithTheAdjustmentAction(){
        val vm=mockk<BoxesViewModel>(relaxed=true)
        val live=MutableStateFlow(BoxesUiState(boxes=listOf(cash.copy(initialBalanceSetAt=null,initialBalance=0.0,currentBalance=0.0)),isLoading=false))
        every{vm.uiState} returns live
        showApp{BoxesScreen({},vm)}
        ui.onNodeWithText("حدد الرصيد الابتدائي").performScrollTo().assertExists();dark(true)
        ui.runOnIdle{live.value=live.value.copy(boxes=listOf(cash))}
        ui.onNodeWithText("حدد الرصيد الابتدائي").assertDoesNotExist();dark(false)
        ui.onNodeWithContentDescription("تعديل الرصيد").performScrollTo().assertIsEnabled()
        verify(exactly=0){vm.setInitialBalance(any(),any(),any())}
    }
}
