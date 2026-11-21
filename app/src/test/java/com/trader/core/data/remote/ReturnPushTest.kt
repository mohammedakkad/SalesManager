package com.trader.core.data.remote

import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.trader.core.domain.model.ReturnInvoice
import com.trader.core.domain.model.ReturnItem
import io.mockk.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*

class ReturnPushTest {
    private val database=mockk<FirebaseDatabase>()
    private val reference=mockk<DatabaseReference>()
    @Before fun setup() {
        mockkStatic(FirebaseDatabase::class)
        every { FirebaseDatabase.getInstance() } returns database
        every { database.reference } returns reference
        every { reference.child(any()) } returns reference
    }
    @After fun cleanup() { unmockkStatic(FirebaseDatabase::class) }
    @Test fun timeoutCannotBeAcknowledgedAsSuccessfulUpload()=runTest {
        every { reference.setValue(any()) } returns TaskCompletionSource<Void>().task
        try {
            FirebaseSyncService().pushReturnInvoice("fixture",ReturnInvoice(id="r",originalTransactionId=8),emptyList())
            fail("Upload timeout silently returned success")
        } catch(expected:TimeoutCancellationException) { }
    }
    @Test fun uploadPreservesReturnedProfitForOtherDevices()=runTest {
        val payload=slot<Any>()
        every { reference.setValue(capture(payload)) } returns TaskCompletionSource<Void>().also { it.setResult(null) }.task
        val item=ReturnItem(id="item",returnInvoiceId="r",returnedQuantity=1.0,pricePerUnit=25.0,costPricePerUnit=21.0)
        FirebaseSyncService().pushReturnInvoice("fixture",ReturnInvoice(id="r",originalTransactionId=8),listOf(item))
        val child=((payload.captured as Map<*,*>)["items"] as Map<*,*>)["item"] as Map<*,*>
        assertEquals(4.0,child["lostProfit"])
        assertEquals(25.0,child["totalRefund"])
        verify { reference.child("r") }
    }
}
