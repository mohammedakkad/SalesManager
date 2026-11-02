package com.trader.core.domain.model
import org.junit.Assert.*
import org.junit.Test

class ReturnItemTest {
    private fun valid() = ReturnItem(productId="p", unitId="u", originalQuantity=5.0, returnedQuantity=2.0, pricePerUnit=10.0, costPricePerUnit=6.0)
    @Test fun partialRefundPreservesFinancialAmounts() {
        val item=valid()
        assertTrue(item.validate().isSuccess)
        assertEquals(20.0,item.totalRefund,0.0)
        assertEquals(8.0,item.lostProfit,0.0)
    }
    @Test fun fullReturnIsAllowed() { assertTrue(valid().copy(returnedQuantity=5.0).validate().isSuccess) }
    @Test fun invalidQuantitiesAreRejected() {
        for(q in listOf(0.0,-1.0,6.0,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY)) {
            assertTrue("quantity $q",valid().copy(returnedQuantity=q).validate().isFailure)
        }
    }
    @Test fun invalidOriginalQuantityIsRejected() {
        for(q in listOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0)) assertTrue(valid().copy(originalQuantity=q).validate().isFailure)
    }
    @Test fun invalidPricesAreRejected() {
        for(p in listOf(-1.0,Double.NaN,Double.POSITIVE_INFINITY)) {
            assertTrue(valid().copy(pricePerUnit=p).validate().isFailure)
            assertTrue(valid().copy(costPricePerUnit=p).validate().isFailure)
        }
    }
    @Test fun blankIdentityIsRejected() {
        assertTrue(valid().copy(productId=" ").validate().isFailure)
        assertTrue(valid().copy(unitId="").validate().isFailure)
    }
    @Test fun freeReturnWithoutCostIsAllowed() {
        assertTrue(valid().copy(pricePerUnit=0.0,costPricePerUnit=0.0).validate().isSuccess)
    }
}
