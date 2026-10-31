package com.trader.core.sync
import com.trader.core.util.NetworkMonitor
import com.trader.core.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReturnRetryCoordinatorTest {
    @Test fun reconnectRetriesReturnsEvenWhenStockUploadFails()=runTest {
        val network=mockk<NetworkMonitor>();val online=MutableStateFlow(false)
        every { network.isOnlineFlow } returns online
        val stock=mockk<StockRepository>(relaxed=true)
        val returns=mockk<ReturnRepository>(relaxed=true)
        coEvery { stock.syncPendingMovements() } throws IOException("fixture")
        val coordinator=SyncCoordinator(network,stock,mockk(relaxed=true),mockk(relaxed=true),mockk(relaxed=true),returns,backgroundScope)
        coordinator.start();runCurrent();coVerify(exactly=0) { returns.syncPendingReturns() }
        online.value=true;runCurrent();coVerify(exactly=1) { returns.syncPendingReturns() }
        online.value=false;runCurrent();online.value=true;runCurrent()
        coVerify(exactly=2) { returns.syncPendingReturns() }
    }
}
