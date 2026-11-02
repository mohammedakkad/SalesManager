package com.trader.salesmanager.sales

import android.app.Application
import com.trader.core.data.remote.FirebaseSyncService
import com.trader.core.domain.repository.ActivationRepository
import com.trader.core.sync.SyncCoordinator
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE, application = Application::class)
class SaleSyncTest {
    private val remote = mockk<FirebaseSyncService>(relaxed = true)
    private val activation = mockk<ActivationRepository>()
    private val coordinator = mockk<SyncCoordinator>(relaxed = true)

    @Test fun dispatchIsScheduledAndUsesCapturedMerchantAfterCommitCallerReturns() = runTest {
        coEvery { activation.getMerchantCode() } returns "fixture"
        SaleSync(remote, activation, coordinator, this).deleted("fixture", 8)
        coVerify(exactly = 0) { remote.deleteTransaction(any(), any()) }
        runCurrent()
        coVerifyOrder {
            remote.deleteInvoiceItemsForTransaction("fixture", 8)
            remote.deleteTransaction("fixture", 8)
            coordinator.syncAll()
        }
    }
    @Test fun switchedMerchantDoesNotDispatchOldSale() = runTest {
        coEvery { activation.getMerchantCode() } returns "other"
        SaleSync(remote, activation, coordinator, this).deleted("fixture", 8)
        runCurrent()
        coVerify(exactly = 0) { remote.deleteInvoiceItemsForTransaction(any(), any()) }
        coVerify(exactly = 0) { remote.deleteTransaction(any(), any()) }
    }
    @Test fun remoteFailureKeepsPendingStockAndCashRetryAvailable() = runTest {
        coEvery { activation.getMerchantCode() } returns "fixture"
        coEvery { remote.deleteInvoiceItemsForTransaction(any(), any()) } throws IllegalStateException("offline")
        SaleSync(remote, activation, coordinator, this).deleted("fixture", 8)
        runCurrent()
        coVerify(exactly = 1) { coordinator.syncAll() }
        coVerify(exactly = 0) { remote.deleteTransaction(any(), any()) }
    }
    @Test fun cancellationStopsRemainingRemoteEffects() = runTest {
        coEvery { activation.getMerchantCode() } returns "fixture"
        coEvery { remote.deleteInvoiceItemsForTransaction(any(), any()) } throws CancellationException("cancelled")
        SaleSync(remote, activation, coordinator, this).deleted("fixture", 8)
        runCurrent()
        coVerify(exactly = 0) { remote.deleteTransaction(any(), any()) }
        coVerify(exactly = 0) { coordinator.syncAll() }
    }
}
