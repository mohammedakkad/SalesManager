package com.trader.salesmanager.ui.coverage

import androidx.compose.ui.test.*
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ProductRepository
import com.trader.salesmanager.ui.inventory.addedit.*
import io.mockk.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductFormUiFlowTest:OfflineSellerUiTest() {
    private val repo=mockk<ProductRepository>(relaxed=true)
    @Test fun blankProductCannotBeSubmitted() {
        val vm=owned(AddEditProductViewModel(repo))
        show { AddEditProductScreen(onNavigateUp={},viewModel=vm) }
        ui.onNodeWithText("إضافة الصنف").performScrollTo().assertIsNotEnabled()
        coVerify(exactly=0) { repo.saveProduct(any(),any(),any()) }
    }
    @Test fun validProductSavesTheUnitPriceCostAndQuantity() {
        val vm=owned(AddEditProductViewModel(repo));var backs=0
        coEvery { repo.saveProduct(any(),any(),any()) } returns "p"
        show { AddEditProductScreen(onNavigateUp={backs++},viewModel=vm) }
        ui.onNode(hasSetTextAction() and hasText("اسم الصنف *")).performTextReplacement("أرز جديد")
        ui.runOnIdle { vm.setCategory("غذاء");vm.updateUnitPrice(0,"25");vm.updateUnitCostPrice(0,"15");vm.updateUnitQty(0,"10") }
        ui.onNodeWithText("إضافة الصنف").performScrollTo().performClick()
        coVerify { repo.saveProduct(match { it.name=="أرز جديد" && it.category=="غذاء" },match { it.single().price==25.0 && it.single().costPrice==15.0 && it.single().quantityInStock==10.0 },any()) }
        ui.mainClock.advanceTimeBy(350)
        ui.waitUntil(3000) { backs==1 }
        Assert.assertEquals(1,backs)
    }
}
