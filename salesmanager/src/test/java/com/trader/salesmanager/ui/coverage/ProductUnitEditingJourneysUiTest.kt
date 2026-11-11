package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ProductRepository
import com.trader.salesmanager.ui.inventory.addedit.*
import io.mockk.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class ProductUnitEditingJourneysUiTest:OfflineSellerUiTest() {
    private val repo=mockk<ProductRepository>(relaxed=true)
    private lateinit var vm:AddEditProductViewModel
    @Before fun setup(){vm=owned(AddEditProductViewModel(repo));coEvery{repo.saveProduct(any(),any(),any())} returns "saved"}
    private fun screen(){show{AddEditProductScreen(onNavigateUp={},viewModel=vm)}}
    private fun field(label:String)=ui.onNode(hasSetTextAction() and hasText(label))
    @Test fun cartonEditorPreservesPackSizePriceCostQuantityAndExplicitZeroThreshold(){
        screen();field("اسم الصنف *").performTextReplacement("كرتون جديد")
        ui.onNodeWithText("كرتون").performScrollTo().performClick()
        field("عدد القطع في الكرتون").performScrollTo().performTextReplacement("12")
        field("سعر البيع ₪").performScrollTo().performTextReplacement("25")
        field("سعر الشراء ₪").performScrollTo().performTextReplacement("20")
        field("الكمية").performScrollTo().performTextReplacement("3")
        field("تنبيه عند نقص الكمية إلى").performScrollTo().performTextReplacement("0")
        ui.onNodeWithText("إضافة الصنف").performScrollTo().assertIsEnabled().performClick()
        ui.waitUntil(5000){vm.uiState.value.savedSuccessfully}
        coVerify(exactly=1){repo.saveProduct(match{it.name=="كرتون جديد"},match{it.single().let{u->u.unitType==UnitType.CARTON&&u.itemsPerCarton==12&&u.price==25.0&&u.costPrice==20.0&&u.quantityInStock==3.0&&u.lowStockThreshold==0.0}},any())}
    }
    @Test fun switchingFromCartonToWeightClearsPackSizeAndStoresKilograms(){
        screen();ui.onNodeWithText("كرتون").performScrollTo().performClick()
        field("عدد القطع في الكرتون").performScrollTo().performTextReplacement("24")
        ui.onNodeWithText("كيلو").performScrollTo().performClick()
        ui.onNodeWithText("وحدة التخزين: كيلوغرام (كجم)").performScrollTo().assertExists()
        ui.onNodeWithText("عدد القطع في الكرتون").assertDoesNotExist()
        field("الكمية (كجم)").performScrollTo().performTextReplacement("1.25")
        ui.runOnIdle{val u=vm.uiState.value.units.single();assertEquals(UnitType.WEIGHT,u.unitType);assertEquals(WeightUnit.KG,u.weightUnit);assertEquals("",u.itemsPerCarton);assertEquals("1.25",u.quantityInStock)}
    }
    @Test fun secondUnitCanBecomeTheOnlyDefaultWithoutReplacingItsIdentity(){
        screen();val first=vm.uiState.value.units.single().id
        ui.onNodeWithText("إضافة وحدة أخرى").performScrollTo().performClick()
        val second=vm.uiState.value.units[1].id
        ui.onNodeWithText("تعيين افتراضي").performScrollTo().performClick()
        ui.runOnIdle{assertEquals(listOf(first,second),vm.uiState.value.units.map{it.id});assertEquals(listOf(second),vm.uiState.value.units.filter{it.isDefault}.map{it.id})}
    }
    @Test fun removingAPersistedUnitUsesItsAccessibleActionAndKeepsTheOtherUnit(){
        val a=ProductUnit("u1","p",unitLabel="أولى",price=10.0,isDefault=true)
        val b=ProductUnit("u2","p",unitLabel="ثانية",price=20.0)
        coEvery{repo.getProductById("p")} returns ProductWithUnits(Product("p",name="صنف قائم"),listOf(a,b))
        vm.loadProduct("p");screen();ui.waitUntil(5000){vm.uiState.value.isEditing}
        ui.onNodeWithContentDescription("حذف الوحدة 2").performScrollTo().performClick()
        ui.runOnIdle{assertEquals(listOf("u1"),vm.uiState.value.units.map{it.id});assertEquals(setOf("u2"),vm.uiState.value.deletedUnitIds)}
        ui.onNodeWithText("حفظ التعديلات").performScrollTo().performClick()
        ui.waitUntil(5000){vm.uiState.value.savedSuccessfully}
        coVerify(exactly=1){repo.saveProduct(match{it.id=="p"},match{it.single().id=="u1"},setOf("u2"))}
    }
    @Test fun marginFeedbackDistinguishesProfitAndLossFromActualEditedPrices(){
        screen();field("سعر البيع ₪").performScrollTo().performTextReplacement("10")
        field("سعر الشراء ₪").performScrollTo().performTextReplacement("20")
        ui.onNodeWithText("خسارة").assertExists();ui.onNodeWithText("₪10.00").assertExists();ui.onNodeWithText("50.0%").assertExists()
        field("سعر البيع ₪").performScrollTo().performTextReplacement("30")
        ui.onNodeWithText("ربح").assertExists();ui.onNodeWithText("خسارة").assertDoesNotExist()
    }
    @Test fun initialBarcodeConflictPreventsSavingAndShowsTheConflictingProduct(){
        coEvery{repo.getBarcodeConflict("ABC",any())} returns "صنف متعارض"
        vm.initWithBarcode("ABC");vm.setName("صنف جديد");vm.updateUnitPrice(0,"10");screen()
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(1,java.util.concurrent.TimeUnit.SECONDS)
        ui.waitUntil(5000){vm.uiState.value.barcodeConflict!=null}
        ui.onNodeWithText("صنف متعارض",substring=true).assertExists()
        ui.onNodeWithText("إضافة الصنف").performScrollTo().assertIsNotEnabled();coVerify(exactly=0){repo.saveProduct(any(),any(),any())}
    }
    @Test fun failedSaveRetainsEditedFieldsAndAllowsAnExplicitSuccessfulRetry(){
        coEvery{repo.saveProduct(any(),any(),any())} throws IOException("تعذر حفظ الاختبار") andThen "saved"
        vm.setName("صنف إعادة المحاولة");vm.updateUnitPrice(0,"15");vm.updateUnitQty(0,"2.5");screen()
        ui.onNodeWithText("إضافة الصنف").performScrollTo().performClick();ui.waitUntil(5000){vm.uiState.value.error!=null}
        ui.onNodeWithText("تعذر حفظ الاختبار",substring=true).performScrollTo().assertExists()
        ui.runOnIdle{assertEquals("صنف إعادة المحاولة",vm.uiState.value.name);assertEquals("15",vm.uiState.value.units.single().price);assertEquals("2.5",vm.uiState.value.units.single().quantityInStock)}
        ui.onNodeWithText("إضافة الصنف").performScrollTo().assertIsEnabled().performClick();ui.waitUntil(5000){vm.uiState.value.savedSuccessfully}
        coVerify(exactly=2){repo.saveProduct(any(),match{it.single().price==15.0&&it.single().quantityInStock==2.5},any())}
    }
}
