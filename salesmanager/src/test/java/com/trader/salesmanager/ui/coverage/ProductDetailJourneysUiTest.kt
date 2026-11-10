package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.salesmanager.ui.inventory.detail.*
import io.mockk.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductDetailJourneysUiTest:OfflineSellerUiTest() {
    private val vm=mockk<ProductDetailViewModel>(relaxed=true)
    private val unit=ProductUnit(id="u",productId="p",unitLabel="وحدة الاختبار",price=25.0,quantityInStock=2.0,lowStockThreshold=3.0,isDefault=true)
    private val product=ProductWithUnits(Product(id="p",name="صنف الجرد",barcode="SKU-5"),listOf(unit))
    private val state=MutableStateFlow(ProductDetailUiState())
    @Before fun fixture(){every{vm.uiState} returns state;every{vm.getMovementsForUnit(any())} returns flowOf(emptyList());state.value=ProductDetailUiState(product=product,isLoading=false)}
    private fun open(){ui.onNodeWithText(unit.unitLabel).performScrollTo().performClick()}
    @Test fun accessibleHeaderNavigatesBackAndEditsTheExactProduct(){
        var back=0;var edited="";show{ProductDetailScreen("p",{back++},{edited=it},vm)}
        ui.onNodeWithContentDescription("رجوع").performClick();assertEquals(1,back)
        ui.onNodeWithContentDescription("تعديل الصنف").performClick();assertEquals("p",edited)
    }
    @Test fun deleteRequiresConfirmationAndWaitsForSuccessfulCompletion(){
        var back=0;var completed:(()->Unit)?=null
        every{vm.deleteProduct(any())} answers{completed=firstArg()}
        show{ProductDetailScreen("p",{back++},{},vm)}
        ui.onNodeWithContentDescription("حذف الصنف").performClick()
        ui.onNodeWithText("إلغاء").performClick();verify(exactly=0){vm.deleteProduct(any())}
        ui.onNodeWithContentDescription("حذف الصنف").performClick();ui.onNodeWithText("حذف",useUnmergedTree=true).performClick()
        assertEquals(0,back);verify(exactly=1){vm.deleteProduct(any())}
        ui.runOnIdle{completed!!()};assertEquals(1,back)
    }
    @Test fun expandedUnitProvidesTheCorrectAddAndDeductActionsAndCollapses(){
        show{ProductDetailScreen("p",{},{},vm)};open()
        ui.onNodeWithText("إضافة").performScrollTo().performClick();verify(exactly=1){vm.showAdjust(unit,true)}
        ui.onNodeWithText("خصم").performScrollTo().performClick();verify(exactly=1){vm.showAdjust(unit,false)}
        open();ui.onNodeWithText("خصم").assertDoesNotExist()
    }
    @Test fun allLedgerMovementKindsKeepTheirNotesAndShowOnlyTenRows(){
        val movements=(0..11).map{index->StockMovement(id="m$index",productId="p",unitId="u",quantity=if(index%2==0)2.0 else -1.0,
            quantityBefore=3.0,quantityAfter=if(index%2==0)5.0 else 2.0,movementType=MovementType.entries[index%5],note="حركة $index")}
        every{vm.getMovementsForUnit("u")} returns flowOf(movements)
        show{ProductDetailScreen("p",{},{},vm)};open()
        for(label in listOf("بيع","إرجاع","إضافة يدوية","خصم يدوي","تعديل جرد","+ 2 حركة أخرى")){
            ui.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(label));ui.onAllNodesWithText(label).onFirst().assertExists()
        }
        ui.onNodeWithText("حركة 10").assertDoesNotExist();ui.onNodeWithText("حركة 11").assertDoesNotExist()
    }
    @Test fun cartonAndWeightUnitsRetainTheirOwnQuantitiesAndPackSize(){
        state.value=state.value.copy(product=product.copy(units=listOf(unit.copy(unitType=UnitType.CARTON,itemsPerCarton=12,quantityInStock=0.0),
            unit.copy(id="w",unitLabel="وزن الاختبار",unitType=UnitType.WEIGHT,quantityInStock=1.25))))
        show{ProductDetailScreen("p",{},{},vm)}
        ui.onNodeWithText("12 قطعة/كرتون").assertExists();ui.onNodeWithText("1.250 كجم").assertExists()
    }
    @Test fun nonFiniteQuantityCannotEnableTheAdjustmentCommand(){
        state.value=state.value.copy(adjustState=ManualAdjustState(show=true,unitId="u",unitLabel=unit.unitLabel,quantity="Infinity"))
        show{ProductDetailScreen("p",{},{},vm)}
        ui.onNodeWithText("تطبيق").assertIsNotEnabled();verify(exactly=0){vm.applyAdjust()}
    }
    @Test fun savingAndInvalidQuantitiesBlockSubmissionWhileValidFiniteInputEnablesIt(){
        state.value=state.value.copy(adjustState=ManualAdjustState(show=true,unitId="u",unitLabel=unit.unitLabel))
        show{ProductDetailScreen("p",{},{},vm)}
        for(value in listOf("","bad","0","-1","NaN")){
            ui.runOnIdle{state.value=state.value.copy(adjustState=state.value.adjustState.copy(quantity=value))}
            ui.onNodeWithText("تطبيق").assertIsNotEnabled()
        }
        ui.runOnIdle{state.value=state.value.copy(adjustState=state.value.adjustState.copy(quantity="1.25"))}
        ui.onNodeWithText("تطبيق").assertIsEnabled().performClick();verify(exactly=1){vm.applyAdjust()}
        ui.runOnIdle{state.value=state.value.copy(isSaving=true)};ui.onNodeWithText("تطبيق").assertIsNotEnabled()
        ui.onNodeWithText("إلغاء").performClick();verify(exactly=1){vm.dismissAdjust()}
    }
}
