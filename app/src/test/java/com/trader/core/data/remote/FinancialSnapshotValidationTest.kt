package com.trader.core.data.remote

import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.database.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

/** Public bulk/listener APIs only; the SDK and all wire values are deterministic and offline. */
@OptIn(ExperimentalCoroutinesApi::class)
class FinancialSnapshotValidationTest {
    private val database=mockk<FirebaseDatabase>()
    private val refs=mutableMapOf<String,DatabaseReference>()
    private val reads=mutableMapOf<String,Any?>()
    private val listeners=mutableMapOf<String,ValueEventListener>()
    private val root="merchants/m1"
    private fun ref(path:String):DatabaseReference=refs.getOrPut(path){mockk<DatabaseReference>().also{r->
        every{r.child(any())} answers{ref(listOf(path,firstArg<String>()).filter{it.isNotEmpty()}.joinToString("/"))}
        every{r.get()} answers{TaskCompletionSource<DataSnapshot>().also{it.setResult(snapshot(path.substringAfterLast('/'),reads[path],path))}.task}
        every{r.addValueEventListener(any())} answers{listeners[path]=firstArg();firstArg()}
        every{r.removeEventListener(any<ValueEventListener>())} just Runs
    }}
    private fun snapshot(key:String,value:Any?,path:String):DataSnapshot=mockk<DataSnapshot>().also{s->
        every{s.key} returns key;every{s.value} returns value;every{s.exists()} returns(value!=null)
        every{s.children} answers{(value as? Map<*,*>)?.entries?.map{snapshot(it.key.toString(),it.value,"$path/${it.key}")}?:emptyList()}
        every{s.child(any())} answers{val k=firstArg<String>();snapshot(k,(value as? Map<*,*>)?.get(k),"$path/$k")}
        every{s.getValue(String::class.java)} returns(value as? String)
    }
    private val transaction=mapOf("id" to 41L,"customerId" to 7L,"amount" to 25.0,"date" to 1L)
    private val line=mapOf("id" to "line","transactionId" to 41L,"productId" to "p","unitId" to "u","quantity" to 2.0,"pricePerUnit" to 12.5,"totalPrice" to 25.0)
    private val returnItem=mapOf("id" to "return-line","productId" to "p","unitId" to "u","originalQuantity" to 2.0,"returnedQuantity" to 0.4,"pricePerUnit" to 12.5,"totalRefund" to 5.0)
    private val returned=mapOf("id" to "return","originalTransactionId" to 41L,"totalRefund" to 5.0,"items" to mapOf("line" to returnItem),"createdAt" to 1L)
    private val cashBox=mapOf("id" to "box","paymentMethodId" to 1L,"currentBalance" to 100.0,"initialBalance" to 80.0,"updatedAt" to 1L)
    private val cashMovement=mapOf("id" to "movement","cashBoxId" to "box","amountDelta" to 20.0,"balanceAfter" to 100.0,"createdAt" to 1L)
    private val nonfinite=listOf("1e309","-Infinity","NaN",Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN)
    @Before fun setup(){
        mockkStatic(FirebaseDatabase::class);every{FirebaseDatabase.getInstance()} returns database;every{database.reference} answers{ref("")}
        reads["$root/invoice_items"]=mapOf("line" to line)
    }
    @After fun cleanup(){unmockkStatic(FirebaseDatabase::class)}
    private suspend fun rejectBulk(family:String,value:Any){
        reads["$root/$family"]=value
        val failure=runCatching{FirebaseSyncService().fetchAllData("m1")}.exceptionOrNull()
        assertTrue("$family must reject the entire nonfinite snapshot instead of returning partial/empty data",failure is IllegalArgumentException)
    }
    private suspend fun TestScope.rejectRealtime(family:String,value:Any){
        val service=FirebaseSyncService()
        val flow:Flow<*> = when(family){
            "transactions"->service.observeTransactions("m1")
            "invoice_items"->service.observeInvoiceItems("m1")
            "return_invoices"->service.observeReturnInvoices("m1")
            "cash_boxes"->service.observeCashBoxes("m1")
            else->service.observeCashBoxMovements("m1")
        }
        val result=async{runCatching{flow.first()}};runCurrent()
        val path="$root/$family";val listener=listeners.getValue(path)
        listener.onDataChange(snapshot(family,value,path));runCurrent()
        val failure=withTimeout(5000){result.await()}.exceptionOrNull()
        assertTrue("$family must close with a typed failure, never an empty/partial replacement",failure is SellerRealtimeException)
        assertEquals(DatabaseError.OPERATION_FAILED,(failure as SellerRealtimeException).databaseCode)
        verify(exactly=1){ref(path).removeEventListener(listener)}
    }
    private fun rows(good:Map<String,Any>,bad:Map<String,Any>)=mapOf("good" to good,"bad" to bad)
    @Test fun bulkTransactionAmountsRejectNonfiniteWireNumbersAsACompleteSnapshot()=runTest {
        for(field in listOf("amount","originalAmount"))for(value in nonfinite)rejectBulk("transactions",rows(transaction,transaction+mapOf("id" to 42L,field to value)))
    }
    @Test fun bulkInvoiceQuantityAndPricesCannotImportNonfiniteValues()=runTest {
        for(field in listOf("quantity","pricePerUnit","totalPrice"))for(value in nonfinite)rejectBulk("invoice_items",rows(line,line+mapOf("id" to "bad",field to value)))
    }
    @Test fun bulkReturnRefundCannotTurnIntoInfiniteMoney()=runTest {
        for(value in nonfinite)rejectBulk("return_invoices",rows(returned,returned+mapOf("id" to "bad","totalRefund" to value)))
    }
    @Test fun bulkNestedReturnItemsRejectNonfiniteQuantitiesCostsAndRefunds()=runTest {
        for(field in listOf("originalQuantity","returnedQty","returnedQuantity","pricePerUnit","costPricePerUnit","lostProfit","totalRefund"))for(value in nonfinite){
            val bad=returned+mapOf("id" to "bad","items" to listOf(returnItem+mapOf(field to value)))
            rejectBulk("return_invoices",rows(returned,bad))
        }
    }
    @Test fun bulkCashBalancesCannotBePartiallyImportedWhenOneIsNonfinite()=runTest {
        for(field in listOf("currentBalance","initialBalance"))for(value in nonfinite)rejectBulk("cash_boxes",rows(cashBox,cashBox+mapOf("id" to "bad",field to value)))
    }
    @Test fun bulkCashMovementsRejectNonfiniteDeltaOrResult()=runTest {
        for(field in listOf("amountDelta","balanceAfter"))for(value in nonfinite)rejectBulk("cash_box_movements",rows(cashMovement,cashMovement+mapOf("id" to "bad",field to value)))
    }
    @Test fun realtimeTransactionAmountsCannotPublishAPartialReplacement()=runTest {
        for(field in listOf("amount","originalAmount"))for(value in nonfinite)rejectRealtime("transactions",rows(transaction,transaction+mapOf("id" to 42L,field to value)))
    }
    @Test fun realtimeInvoiceQuantitiesAndPricesCannotPublishNonfiniteValues()=runTest {
        for(field in listOf("quantity","pricePerUnit","totalPrice"))for(value in nonfinite)rejectRealtime("invoice_items",rows(line,line+mapOf("id" to "bad",field to value)))
    }
    @Test fun realtimeReturnRefundRejectsTheWholeSnapshot()=runTest {
        for(value in nonfinite)rejectRealtime("return_invoices",rows(returned,returned+mapOf("id" to "bad","totalRefund" to value)))
    }
    @Test fun realtimeNestedReturnAmountsCannotBecomeADeletedOrZeroedReturn()=runTest {
        for(field in listOf("originalQuantity","returnedQty","returnedQuantity","pricePerUnit","costPricePerUnit","lostProfit","totalRefund"))for(value in nonfinite){
            val bad=returned+mapOf("id" to "bad","items" to listOf(returnItem+mapOf(field to value)))
            rejectRealtime("return_invoices",rows(returned,bad))
        }
    }
    @Test fun realtimeCashBalancesRejectAllRowsWhenOneIsNonfinite()=runTest {
        for(field in listOf("currentBalance","initialBalance"))for(value in nonfinite)rejectRealtime("cash_boxes",rows(cashBox,cashBox+mapOf("id" to "bad",field to value)))
    }
    @Test fun realtimeCashMovementsRejectNonfiniteDeltaAndBalance()=runTest {
        for(field in listOf("amountDelta","balanceAfter"))for(value in nonfinite)rejectRealtime("cash_box_movements",rows(cashMovement,cashMovement+mapOf("id" to "bad",field to value)))
    }
    @Test fun returnFallbackInvoiceCannotSupplyANonfiniteOriginalQuantityOrUnitPrice()=runTest {
        val needsFallback=returned+mapOf("items" to listOf(mapOf("productId" to "p","returnedQuantity" to 0.4,"totalRefund" to 5.0)))
        for(field in listOf("quantity","pricePerUnit"))for(value in nonfinite){
            reads["$root/invoice_items"]=mapOf("line" to line+mapOf(field to value))
            rejectRealtime("return_invoices",mapOf("return" to needsFallback))
        }
    }
    @Test fun finiteStringAndNumberFormsKeepTheExistingOptionalDefaults()=runTest {
        reads["$root/transactions"]=mapOf("41" to transaction+mapOf("amount" to "25.50"))
        reads["$root/cash_boxes"]=mapOf("box" to cashBox+mapOf("currentBalance" to -20L))
        val data=FirebaseSyncService().fetchAllData("m1")
        assertEquals(25.5,data.transactions.single().amount,0.0);assertEquals(25.5,data.transactions.single().originalAmount,0.0)
        assertEquals(-20.0,data.cashBoxes.single().currentBalance,0.0);assertEquals(2.0,data.invoiceItems.single().quantity,0.0)
    }
    @Test fun unknownMetadataAndAbsentOptionalFinancialFieldsRetainLegacyMeaning()=runTest {
        reads["$root/transactions"]=mapOf("41" to transaction+mapOf("metadata" to mapOf("amount" to "not a financial field")))
        reads["$root/cash_boxes"]=mapOf("box" to mapOf("id" to "box","paymentMethodId" to 1L))
        val data=FirebaseSyncService().fetchAllData("m1")
        assertEquals(25.0,data.transactions.single().amount,0.0);assertEquals(0.0,data.cashBoxes.single().currentBalance,0.0);assertEquals(0.0,data.cashBoxes.single().initialBalance,0.0)
    }
}
