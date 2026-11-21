package com.trader.core.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.domain.usecase.GetDashboardAnalyticsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class DashboardCalendarQualificationTest {
    private lateinit var db:AppDatabase;private lateinit var oldZone:TimeZone
    private fun date(month:Int,day:Int,hour:Int=0)=Calendar.getInstance().apply{clear();set(2026,month,day,hour,0)}.timeInMillis
    @Before fun open()=runBlocking{
        oldZone=TimeZone.getDefault();TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),AppDatabase::class.java).allowMainThreadQueries().build()
        db.customerDao().insertAll(listOf(CustomerEntity(1,"أول"),CustomerEntity(2,"ثان")))
        db.transactionDao().insertAll(listOf(TransactionEntity(11,1,100.0,true,date=date(10,9,12)),TransactionEntity(12,2,50.0,false,date=date(10,9,23)),TransactionEntity(13,1,200.0,true,date=date(10,1)),TransactionEntity(14,1,40.0,false,date=date(9,31))))
        db.invoiceItemDao().insertAll(listOf(InvoiceItemEntity("a",11,"p","أرز","u","كيس",2.0,50.0,100.0,"m1","SYNCED"),InvoiceItemEntity("b",14,"q","قديم","v","حبة",100.0,0.4,40.0,"m1","SYNCED")))
    }
    @After fun close(){db.close();TimeZone.setDefault(oldZone)}
    @Test fun realDashboardSeparatesTodayMonthDebtAndSevenDayWindows()=runBlocking {
        val flow=GetDashboardAnalyticsUseCase(DashboardAnalyticsRepositoryImpl(db.transactionDao(),db.invoiceItemDao()))(date(10,9,12))
        val report=flow.first();assertEquals(150.0,report.todaySummary.totalSales,0.0);assertEquals(2,report.todaySummary.invoiceCount)
        assertEquals(100.0,report.todaySummary.paidSales,0.0);assertEquals(50.0,report.todaySummary.unpaidSales,0.0);assertEquals(90.0,report.totalOutstandingDebt,0.0)
        assertEquals(listOf("p"),report.topSellingProducts.map{it.productId});assertEquals(listOf(2L,1L),report.topDebtorCustomers.map{it.customerId})
        assertEquals(7,report.lastSevenDaysSales.size);assertEquals(date(10,3),report.lastSevenDaysSales.first().dayStartMillis);assertEquals(150.0,report.lastSevenDaysSales.sumOf{it.totalSales},0.0)
    }
}
