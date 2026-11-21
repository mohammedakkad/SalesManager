package com.trader.salesmanager.ui.settings.backup

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.CustomerEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class BackupExportCancellationRoomTest {
    private val context:Context=ApplicationProvider.getApplicationContext()
    private lateinit var db:AppDatabase
    @Before fun open() { db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).allowMainThreadQueries().build() }
    @After fun close() { db.close() }
    @Test fun cancelledSessionLookupPropagatesAndReleasesSnapshotTransaction()=runTest {
        val customer=CustomerEntity(7,"fixture")
        db.customerDao().insertAll(listOf(customer))
        val cancelled=CancellationException("fixture cancellation")
        val before=context.cacheDir.listFiles().orEmpty().map{it.name}.toSet()
        val manager=BackupManager(context,db,"tenant",currentMerchant={throw cancelled})
        try { manager.exportEncryptedBackup("fixture".toCharArray());fail("Cancelled export completed") }
        catch(actual:CancellationException) { assertEquals(cancelled.message,actual.message) }
        assertFalse(db.inTransaction());assertEquals(listOf(customer),db.customerDao().getAllOnce())
        assertEquals(before,context.cacheDir.listFiles().orEmpty().map{it.name}.toSet())
    }
}
