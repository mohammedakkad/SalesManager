package com.trader.core.domain.model
import org.junit.Assert.*
import org.junit.Test
class ProductStockTest {
    @Test fun stockBoundaryClassification() {
        val unit=ProductUnit(lowStockThreshold=2.0)
        assertEquals(StockLevel.OUT,unit.copy(quantityInStock=-1.0).stockLevel)
        assertEquals(StockLevel.OUT,unit.stockLevel)
        assertEquals(StockLevel.LOW,unit.copy(quantityInStock=2.0).stockLevel)
        assertEquals(StockLevel.AVAILABLE,unit.copy(quantityInStock=2.1).stockLevel)
    }
    @Test fun aggregateAttentionIncludesOutOfStockUnits() {
        val product=ProductWithUnits(Product(),listOf(ProductUnit(quantityInStock=0.0)))
        assertTrue(product.needsStockAttention)
        assertTrue(product.isOutOfStock)
        assertFalse(product.isLowStock)
        assertFalse(product.copy(units=emptyList()).isOutOfStock)
        assertNull(product.copy(units=emptyList()).defaultUnit)
    }
    @Test fun explicitDefaultWinsOtherwiseFirst() {
        val first=ProductUnit(id="first")
        val chosen=ProductUnit(id="chosen",isDefault=true)
        assertEquals(chosen,ProductWithUnits(Product(),listOf(first,chosen)).defaultUnit)
        assertEquals(first,ProductWithUnits(Product(),listOf(first)).defaultUnit)
    }
    @Test fun profitIncludesLossButUnspecifiedCostIsUnknown() {
        assertNull(ProductUnit(price=10.0).profitMargin)
        assertEquals(-2.0,ProductUnit(price=8.0,costPrice=10.0).profitMargin!!,0.0)
        assertEquals(25.0,ProductUnit(price=10.0,costPrice=8.0).profitPercent!!,0.0)
    }
}
