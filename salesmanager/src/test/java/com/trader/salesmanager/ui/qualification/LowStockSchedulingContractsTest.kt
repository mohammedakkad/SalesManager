package com.trader.salesmanager.ui.qualification

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.*
import androidx.work.impl.WorkManagerImpl
import androidx.work.testing.WorkManagerTestInitHelper
import com.trader.salesmanager.worker.LowStockCheckWorker
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class LowStockSchedulingContractsTest {
    private val app:Application=ApplicationProvider.getApplicationContext()
    private var manager:WorkManager?=null
    private fun scheduler():WorkManager {
        if(manager==null){WorkManagerTestInitHelper.initializeTestWorkManager(app,Configuration.Builder().setExecutor(Executor{it.run()}).setTaskExecutor(Executor{it.run()}).build());manager=WorkManager.getInstance(app)}
        return manager!!
    }
    @After fun close(){manager?.let{it.cancelAllWork().result.get();(it as WorkManagerImpl).workDatabase.close();WorkManagerImpl.setDelegate(null)}}
    private fun at(zone:String,year:Int,month:Int,day:Int,hour:Int,minute:Int=0)=Calendar.getInstance(TimeZone.getTimeZone(zone)).apply{clear();set(year,month,day,hour,minute)}.timeInMillis
    private fun delay(hour:Int,now:Long,zone:String="UTC")=LowStockCheckWorker.initialDelayMillis(hour,now,TimeZone.getTimeZone(zone))
    @Test fun futureHourIsScheduledTodayAndRemovesMinutesSecondsAndMilliseconds(){
        val now=at("UTC",2026,0,1,6,15)+59_123
        assertEquals(TimeUnit.HOURS.toMillis(2)-TimeUnit.MINUTES.toMillis(15)-59_123,delay(8,now))
    }
    @Test fun exactPreferredHourSchedulesTomorrowInsteadOfImmediateDuplicate(){assertEquals(TimeUnit.DAYS.toMillis(1),delay(8,at("UTC",2026,0,1,8)))}
    @Test fun pastPreferredHourSchedulesTheNextDay(){assertEquals(TimeUnit.HOURS.toMillis(23),delay(8,at("UTC",2026,0,1,9)))}
    @Test fun springDstTransitionPreservesTheLocalHour(){val z="America/New_York";assertEquals(TimeUnit.HOURS.toMillis(22),delay(8,at(z,2026,2,7,9),z))}
    @Test fun autumnDstTransitionPreservesTheLocalHour(){val z="America/New_York";assertEquals(TimeUnit.HOURS.toMillis(24),delay(8,at(z,2026,9,31,9),z))}
    @Test fun repeatedSchedulingKeepsOneExistingPeriodicRequest(){
        val m=scheduler();LowStockCheckWorker.schedule(app,8);val first=m.getWorkInfosForUniqueWork(LowStockCheckWorker.WORK_NAME).get().single()
        LowStockCheckWorker.schedule(app,9);val rows=m.getWorkInfosForUniqueWork(LowStockCheckWorker.WORK_NAME).get();assertEquals(1,rows.size);assertEquals(first.id,rows.single().id);assertFalse(rows.single().state.isFinished)
        val spec=(m as WorkManagerImpl).workDatabase.workSpecDao().getWorkSpec(first.id.toString())!!
        assertEquals(TimeUnit.HOURS.toMillis(24),spec.intervalDuration)
    }
    @Test fun explicitReplacementCancelsTheOldRequestAndCreatesOneActiveRequest(){
        val m=scheduler();LowStockCheckWorker.schedule(app,8);val first=m.getWorkInfosForUniqueWork(LowStockCheckWorker.WORK_NAME).get().single()
        LowStockCheckWorker.schedule(app,9,replaceExisting=true);val rows=m.getWorkInfosForUniqueWork(LowStockCheckWorker.WORK_NAME).get();val active=rows.filterNot{it.state.isFinished};assertEquals(1,active.size);assertNotEquals(first.id,active.single().id)
        val old=m.getWorkInfoById(first.id).get();assertTrue(old==null||old.state==WorkInfo.State.CANCELLED)
    }
    @Test fun cancellationCancelsTheScheduledUniqueWork(){
        val m=scheduler();LowStockCheckWorker.schedule(app,8);LowStockCheckWorker.cancel(app)
        val rows=m.getWorkInfosForUniqueWork(LowStockCheckWorker.WORK_NAME).get();assertEquals(1,rows.size);assertEquals(WorkInfo.State.CANCELLED,rows.single().state)
    }
}
