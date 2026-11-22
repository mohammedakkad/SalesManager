package com.trader.core.data.local

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.database.*
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.data.remote.*
import com.trader.core.data.repository.*
import com.trader.core.domain.repository.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class NonfiniteCacheIntegrityRoomTest {
    private lateinit var db:AppDatabase
    private val context:Context=ApplicationProvider.getApplicationContext()
    private val prefs=MutableStateFlow<Preferences>(emptyPreferences())
    private val store=object:DataStore<Preferences>{
        override val data:Flow<Preferences> = prefs
        override suspend fun updateData(transform:suspend(Preferences)->Preferences)=transform(prefs.value).also{prefs.value=it}
    }
    private val database=mockk<FirebaseDatabase>()
    private val refs=mutableMapOf<String,DatabaseReference>()
    private val reads=mutableMapOf<String,Any?>()
    private val listeners=mutableMapOf<String,ValueEventListener>()
    private val detached=CompletableDeferred<Unit>()
    private val root="merchants/m1"
    private fun ref(path:String):DatabaseReference=refs.getOrPut(path){mockk<DatabaseReference>().also{r->
        every{r.child(any())} answers{ref(listOf(path,firstArg<String>()).filter{it.isNotEmpty()}.joinToString("/"))}
        every{r.get()} answers{TaskCompletionSource<DataSnapshot>().also{it.setResult(snapshot(path.substringAfterLast('/'),reads[path],path))}.task}
        every{r.addValueEventListener(any())} answers{listeners[path]=firstArg();firstArg()}
        every{r.removeEventListener(any<ValueEventListener>())} answers{detached.complete(Unit);Unit}
    }}
    private fun snapshot(key:String,value:Any?,path:String):DataSnapshot=mockk<DataSnapshot>().also{s->
        every{s.key} returns key;every{s.value} returns value;every{s.exists()} returns(value!=null)
        every{s.children} answers{(value as? Map<*,*>)?.entries?.map{snapshot(it.key.toString(),it.value,"$path/${it.key}")}?:emptyList()}
        every{s.child(any())} answers{val k=firstArg<String>();snapshot(k,(value as? Map<*,*>)?.get(k),"$path/$k")}
        every{s.getValue(String::class.java)} returns(value as? String)
    }
    @Before fun setup()=runBlocking {
        mockkStatic("com.trader.core.data.local.DataStoreProviderKt");every{any<Context>().appDataStore} returns store
        mockkStatic(FirebaseDatabase::class);every{FirebaseDatabase.getInstance()} returns database;every{database.reference} answers{ref("")}
        db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).allowMainThreadQueries().build()
        db.customerDao().insertCustomer(CustomerEntity(1,"local customer",createdAt=1))
        db.transactionDao().insertTransaction(TransactionEntity(id=41,customerId=1,amount=25.0,isPaid=false,date=1))
        db.cashBoxDao().upsert(CashBoxEntity("box",1,"cash",currentBalance=100.0,merchantId="m1",updatedAt=1))
        reads["$root/customers"]=mapOf("1" to mapOf("id" to 1L,"name" to "remote replacement","createdAt" to 1L))
        reads["$root/transactions"]=mapOf("41" to mapOf("id" to 41L,"customerId" to 1L,"amount" to 25.0,"date" to 1L))
        Unit
    }
    @After fun close(){db.close();unmockkStatic(FirebaseDatabase::class);unmockkStatic("com.trader.core.data.local.DataStoreProviderKt")}
    private fun snapshotRows()=listOf("customers","transactions","cash_boxes","cash_box_movements","invoice_items","products","product_units","stock_movements","return_invoices","return_items","sale_operations","sale_outbox").associateWith{table->
        db.openHelper.writableDatabase.query("SELECT * FROM $table ORDER BY rowid").use{c->buildList{while(c.moveToNext())add((0 until c.columnCount).map{if(c.isNull(it))null else c.getString(it)})}}
    }
    private fun activation():ActivationRepositoryImpl {
        val products=mockk<ProductFirestoreService>();coEvery{products.fetchAllProducts("m1")} returns emptyList()
        return ActivationRepositoryImpl(context,FirebaseSyncService(),db.customerDao(),db.transactionDao(),db.paymentMethodDao(),db.productDao(),products,db.returnDao(),db.invoiceItemDao(),db.cashBoxDao(),db.cashBoxMovementDao(),db)
    }
    @Test fun infiniteRemoteSaleCannotOverwriteAnyStagedCacheOrDerivedDebt()=runBlocking {
        reads["$root/transactions"]=mapOf("41" to mapOf("id" to 41L,"customerId" to 1L,"amount" to "1e309","date" to 1L))
        val before=snapshotRows();activation().saveActivationStatus(true,"m1")
        assertEquals("Nonfinite sale changed a complete local cache",before,snapshotRows());assertEquals(25.0,db.transactionDao().getUnpaidAmountByCustomer(1),0.0)
    }
    @Test fun infiniteRemoteCashBalanceCannotPartiallyImportEarlierCustomerAndSaleRows()=runBlocking {
        reads["$root/cash_boxes"]=mapOf("box" to mapOf("id" to "box","paymentMethodId" to 1L,"currentBalance" to "1e309","updatedAt" to 1L))
        val before=snapshotRows();activation().saveActivationStatus(true,"m1")
        assertEquals("Nonfinite cash snapshot partially replaced earlier cache rows",before,snapshotRows());assertEquals(100.0,db.cashBoxDao().getById("box")!!.currentBalance,0.0)
    }
    @Test fun invalidRealtimeReturnBatchCannotReplaceOrDeleteExistingRefundRows()=runTest {
        db.returnDao().insertReturnWithItems(ReturnInvoiceEntity("kept",41,"m1","PARTIAL",5.0,"original",1,"SYNCED"),emptyList())
        db.returnDao().insertReturnWithItems(ReturnInvoiceEntity("untouched",41,"m1","PARTIAL",2.0,"retained",2,"SYNCED"),emptyList())
        val before=snapshotRows();val actor=mockk<ActivationRepository>();every{actor.observeMerchantCode()} returns MutableStateFlow("m1")
        val repo=ReturnRepositoryImpl(db,mockk(relaxed=true),mockk(relaxed=true),mockk(relaxed=true),"m1",actor,FirebaseSyncService(),backgroundScope)
        runCurrent();val path="$root/return_invoices";val listener=listeners.getValue(path)
        val bad=mapOf("kept" to mapOf("id" to "kept","originalTransactionId" to 41L,"totalRefund" to "Infinity"),"sentinel" to mapOf("id" to "sentinel","originalTransactionId" to 41L,"totalRefund" to 1.0))
        listener.onDataChange(snapshot("return_invoices",bad,path));runCurrent()
        // Observe either the typed listener shutdown or the baseline's unsafe write.
        // No sleeps, private hooks, fake deletion event or timeout relaxation.
        withContext(Dispatchers.IO) {
            withTimeout(10000){while(!detached.isCompleted && db.returnDao().getById("sentinel")==null)yield()}
        }
        assertEquals("Rejected return batch overwrote or deleted real Room refunds",before,snapshotRows())
        assertTrue("Malformed listener must terminate without an empty snapshot",detached.isCompleted)
        assertEquals(setOf("kept","untouched"),repo.getAllReturns().first().map{it.id}.toSet())
    }
}
