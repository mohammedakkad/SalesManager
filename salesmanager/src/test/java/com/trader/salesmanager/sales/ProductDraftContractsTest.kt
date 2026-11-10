package com.trader.salesmanager.sales

import androidx.lifecycle.viewModelScope
import com.trader.core.domain.model.*
import com.trader.core.domain.repository.ProductRepository
import com.trader.salesmanager.ui.inventory.addedit.AddEditProductViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDraftContractsTest {
    private val repository=mockk<ProductRepository>()
    private lateinit var vm:AddEditProductViewModel
    @Before fun setup(){Dispatchers.setMain(StandardTestDispatcher());vm=AddEditProductViewModel(repository)}
    @After fun close(){vm.viewModelScope.cancel();Dispatchers.resetMain()}
    @Test fun removingAnUnsavedDraftDoesNotRequestRemoteUnitDeletion()=runTest {
        vm.setName("fixture");vm.updateUnitPrice(0,"12")
        vm.addUnit();vm.removeUnit(1)
        assertTrue(vm.uiState.value.deletedUnitIds.isEmpty())
        coEvery { repository.saveProduct(any(),any(),emptySet()) } returns "saved"
        vm.save();advanceUntilIdle()
        assertTrue(vm.uiState.value.savedSuccessfully)
        coVerify(exactly=1){repository.saveProduct(any(),any(),emptySet())}
    }

    private suspend fun rejectOverflowInput(update:(String)->Unit) {
        vm.setName("fixture");vm.updateUnitPrice(0,"12")
        update("9".repeat(400))
        assertFalse("An overflowed numeric field must not be a valid product draft",vm.uiState.value.isValid)
        vm.save()
        coVerify(exactly=0){repository.saveProduct(any(),any(),any())}
    }
    @Test fun overflowingSalePriceCannotBeSaved()=runTest {rejectOverflowInput{vm.updateUnitPrice(0,it)}}
    @Test fun overflowingCostPriceCannotBeSaved()=runTest {rejectOverflowInput{vm.updateUnitCostPrice(0,it)}}
    @Test fun overflowingStockQuantityCannotBeSaved()=runTest {rejectOverflowInput{vm.updateUnitQty(0,it)}}
    @Test fun overflowingLowStockThresholdCannotBeSaved()=runTest {rejectOverflowInput{vm.updateUnitLowStock(0,it)}}

    @Test fun typedZeroThresholdRemainsZeroRatherThanDefaultFive()=runTest {
        vm.setName("fixture");vm.updateUnitPrice(0,"12");vm.updateUnitLowStock(0,"0")
        val saved=slot<List<ProductUnit>>()
        coEvery {repository.saveProduct(any(),capture(saved),any())} returns "saved"
        vm.save();advanceUntilIdle()
        assertTrue(vm.uiState.value.savedSuccessfully)
        assertEquals(0.0,saved.captured.single().lowStockThreshold,0.0)
    }

    @Test fun loadedProductPreservesEditableUnitTypesDefaultsAndWeights()=runTest {
        val units=listOf(ProductUnit(id="a",productId="p",unitLabel="piece",price=25.0,costPrice=10.0,quantityInStock=5.0,lowStockThreshold=2.0),
            ProductUnit(id="b",productId="p",unitType=UnitType.WEIGHT,unitLabel="وزن",price=50.0,isDefault=true,weightUnit=WeightUnit.OZ))
        coEvery {repository.getProductById("p")} returns ProductWithUnits(Product(id="p",name="fixture",category="category",barcode="123"),units)
        vm.loadProduct("p");advanceUntilIdle()
        val state=vm.uiState.value
        assertTrue(state.isEditing);assertEquals("p",state.productId);assertEquals("123",state.barcode)
        assertEquals("category",state.category);assertTrue(state.isValid)
        assertEquals("10.0",state.units[0].costPrice);assertEquals("5.0",state.units[0].quantityInStock)
        assertEquals("2.0",state.units[0].lowStockThreshold)
        assertEquals("",state.units[1].costPrice);assertEquals("",state.units[1].itemsPerCarton)
        assertEquals(WeightUnit.OZ,state.units[1].weightUnit);assertEquals(UnitType.WEIGHT,state.units[1].unitType)
        assertEquals(listOf(false,true),state.units.map{it.isDefault})
    }
    @Test fun removingPersistedDefaultPromotesRemainingUnitAndDeletesOnlyPersistedIdentity()=runTest {
        coEvery {repository.getProductById("p")} returns ProductWithUnits(Product(id="p",name="fixture"),listOf(
            ProductUnit(id="a",unitLabel="piece",price=12.0,isDefault=true),ProductUnit(id="b",unitLabel="piece",price=13.0)))
        vm.loadProduct("p");advanceUntilIdle();vm.addUnit()
        vm.removeUnit(0);vm.removeUnit(1)
        assertEquals(setOf("a"),vm.uiState.value.deletedUnitIds)
        assertEquals("b",vm.uiState.value.units.single().id)
        assertTrue(vm.uiState.value.units.single().isDefault)
        coEvery {repository.saveProduct(any(),any(),setOf("a"))} returns "p"
        vm.save();advanceUntilIdle()
        coVerify {repository.saveProduct(match{it.id=="p"},match{it.single().id=="b"&&it.single().isDefault},setOf("a"))}
    }
    @Test fun numericEditingPreservesFiniteValuesAndBlankOptionalDefaults()=runTest {
        vm.setName("  fixture  ");vm.setCategory("  category  ")
        vm.updateUnitPrice(0,"00012..50abc");vm.updateUnitCostPrice(0,"0002..75abc")
        vm.updateUnitQty(0,"0003.50");vm.updateUnitLowStock(0,"")
        vm.updateUnit(0,vm.uiState.value.units.single().copy(unitType=UnitType.CARTON,unitLabel=" box ",itemsPerCarton="24"))
        val saved=slot<List<ProductUnit>>()
        coEvery {repository.saveProduct(any(),capture(saved),any())} returns "p"
        vm.save();advanceUntilIdle()
        val unit=saved.captured.single()
        assertEquals(12.5,unit.price,0.0);assertEquals(2.75,unit.costPrice,0.0)
        assertEquals(3.5,unit.quantityInStock,0.0);assertEquals(5.0,unit.lowStockThreshold,0.0)
        assertEquals(24,unit.itemsPerCarton);assertEquals("box",unit.unitLabel);assertEquals(UnitType.CARTON,unit.unitType)
        coVerify {repository.saveProduct(match{it.name=="fixture"&&it.category=="category"&&it.barcode==null},any(),emptySet())}
    }
}
