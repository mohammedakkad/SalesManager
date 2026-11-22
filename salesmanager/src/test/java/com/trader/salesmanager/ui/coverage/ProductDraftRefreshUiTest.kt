package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ProductRepository
import com.trader.salesmanager.ui.inventory.addedit.*
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers="w480dp-h900dp")
class ProductDraftRefreshUiTest:AppThemeUiFixture(){
    private val repo=mockk<ProductRepository>(relaxed=true)
    private lateinit var vm:AddEditProductViewModel
    @Before fun model(){vm=owned(AddEditProductViewModel(repo));coEvery{repo.saveProduct(any(),any(),any())} returns "saved"}
    private fun render(){showApp{AddEditProductScreen(onNavigateUp={},viewModel=vm)}}
    private fun field(label:String)=ui.onNode(hasSetTextAction() and hasText(label))
    @Test fun editedCartonFieldsKeepTheirValuesAndMarginAcrossThemeChanges(){
        render();field("اسم الصنف *").performTextReplacement("عبوة متجددة")
        ui.onNodeWithText("كرتون").performScrollTo().performClick()
        field("عدد القطع في الكرتون").performScrollTo().performTextReplacement("18")
        field("سعر البيع ₪").performScrollTo().performTextReplacement("36")
        field("سعر الشراء ₪").performScrollTo().performTextReplacement("18")
        dark(true);ui.onNodeWithText("ربح").assertExists()
        field("الكمية").performScrollTo().performTextReplacement("4.5");dark(false)
        field("الكمية").assertTextContains("4.5")
        ui.runOnIdle{val u=vm.uiState.value.units.single();assertEquals("18",u.itemsPerCarton);assertEquals("36",u.price);assertEquals("18",u.costPrice);assertEquals(UnitType.CARTON,u.unitType)}
        coVerify(exactly=0){repo.saveProduct(any(),any(),any())}
    }
    @Test fun loadedProductReplacementUsesItsOwnUnitAndNameAfterThemeChange(){
        val a=ProductWithUnits(Product("a",name="سابق"),listOf(ProductUnit("ua","a",unitLabel="حبة سابقة",price=10.0,isDefault=true)))
        val b=ProductWithUnits(Product("b",name="بديل"),listOf(ProductUnit("ub","b",unitLabel="كيلو جديد",unitType=UnitType.WEIGHT,price=35.0,weightUnit=WeightUnit.KG,isDefault=true)))
        coEvery{repo.getProductById("a")} returns a;coEvery{repo.getProductById("b")} returns b
        vm.loadProduct("a");render();ui.waitUntil(5000){ui.waitForIdle();vm.uiState.value.productId=="a"};dark(true)
        ui.runOnIdle{vm.loadProduct("b")};ui.waitUntil(5000){ui.waitForIdle();vm.uiState.value.productId=="b"};dark(false)
        field("اسم الصنف *").performScrollTo().assertTextContains("بديل")
        ui.onNodeWithText("وحدة التخزين: كيلوغرام (كجم)").performScrollTo().assertExists()
        ui.runOnIdle{assertEquals("ub",vm.uiState.value.units.single().id);assertEquals("35.0",vm.uiState.value.units.single().price)}
        coVerify(exactly=0){repo.saveProduct(any(),any(),any())}
    }
    @Test fun removedPersistedUnitStaysDeletedAndRemainingDefaultIdentitySurvivesTheme(){
        val a=ProductUnit("u1","p",unitLabel="أولى",price=10.0,isDefault=true)
        val b=ProductUnit("u2","p",unitLabel="ثانية",price=20.0)
        coEvery{repo.getProductById("p")} returns ProductWithUnits(Product("p",name="صنف ثابت"),listOf(a,b))
        vm.loadProduct("p");render();ui.waitUntil(5000){ui.waitForIdle();vm.uiState.value.isEditing}
        ui.onNodeWithContentDescription("حذف الوحدة 2").performScrollTo().performClick();dark(true);dark(false)
        ui.onAllNodesWithContentDescription("حذف الوحدة 2").assertCountEquals(0)
        ui.runOnIdle{assertEquals(setOf("u2"),vm.uiState.value.deletedUnitIds);assertEquals("u1",vm.uiState.value.units.single().id);assertTrue(vm.uiState.value.units.single().isDefault)}
        coVerify(exactly=0){repo.saveProduct(any(),any(),any())}
    }
    @Test fun pendingSaveCannotBeResubmittedAfterThemeChangeAndCompletesOnlyOnce(){
        val pending=CompletableDeferred<String>();coEvery{repo.saveProduct(any(),any(),any())} coAnswers {pending.await()}
        vm.setName("صنف مؤجل");vm.updateUnitPrice(0,"17");render()
        ui.onNodeWithText("إضافة الصنف").performScrollTo().performClick()
        ui.waitUntil(5000){ui.waitForIdle();vm.uiState.value.isSaving};dark(true);dark(false)
        coVerify(exactly=1){repo.saveProduct(any(),any(),any())}
        ui.runOnIdle{pending.complete("saved")};ui.waitUntil(5000){ui.waitForIdle();vm.uiState.value.savedSuccessfully}
        coVerify(exactly=1){repo.saveProduct(any(),any(),any())}
    }
}
