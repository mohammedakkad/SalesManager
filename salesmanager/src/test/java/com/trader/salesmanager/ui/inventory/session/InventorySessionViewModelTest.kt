package com.trader.salesmanager.ui.inventory.session

import androidx.lifecycle.ViewModelStore
import com.trader.core.domain.model.InventorySessionItem
import com.trader.core.domain.repository.InventoryRepository
import com.trader.core.domain.repository.ProductRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class InventorySessionViewModelTest {
    private val repository = mockk<InventoryRepository>(relaxed = true)
    private val store = ViewModelStore()

    @Before fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { repository.getActiveSession() } returns flowOf(null)
        every { repository.getAllSessions() } returns flowOf(emptyList())
    }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    @Test fun validCountsAndClearingPersistWhileInvalidCountsNeverReachRepository() = runTest {
        val model = InventorySessionViewModel(repository, mockk<ProductRepository>(), "m")
        store.put("inventory", model)
        val item = InventorySessionItem(id = "item", sessionId = "session", unitId = "unit", systemQuantity = 5.0)
        for (invalid in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            model.updateItemCount(item, invalid)
        }
        runCurrent()
        coVerify(exactly = 0) { repository.updateSessionItem(any()) }

        model.updateItemCount(item, 0.0); model.updateItemCount(item, 2.5); model.clearItemCount(item)
        runCurrent()
        val updates = mutableListOf<InventorySessionItem>()
        coVerify(exactly = 3) { repository.updateSessionItem(capture(updates)) }
        assertEquals(listOf(0.0, 2.5, null), updates.map { it.actualQuantity })
        assertTrue(updates.all { it.copy(actualQuantity = null) == item })
    }
}
