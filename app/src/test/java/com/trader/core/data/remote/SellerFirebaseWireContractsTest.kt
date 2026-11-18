package com.trader.core.data.remote

import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.database.*
import com.trader.core.domain.model.*
import com.trader.core.domain.model.Transaction
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

/** Actual serializer/listener code against SDK fakes. This does not test Firebase authorization rules. */
@OptIn(ExperimentalCoroutinesApi::class)
class SellerFirebaseWireContractsTest {
    private val database=mockk<FirebaseDatabase>()
    private val refs=mutableMapOf<String,DatabaseReference>()
    private val reads=mutableMapOf<String,Any?>()
    private val writes=mutableMapOf<String,Any?>()
    private val listeners=mutableMapOf<String,ValueEventListener>()
    private val removed=mutableSetOf<String>()
    private val success get()=TaskCompletionSource<Void>().also{it.setResult(null)}.task
    private fun ref(path:String):DatabaseReference=refs.getOrPut(path) {
        mockk<DatabaseReference>(relaxed=true).also { node ->
            every{node.child(any())} answers {ref(listOf(path,firstArg<String>()).filter{it.isNotEmpty()}.joinToString("/"))}
            every{node.orderByChild(any())} returns node;every{node.equalTo(any<String>())} returns node;every{node.equalTo(any<Double>())} returns node
            every{node.get()} answers {TaskCompletionSource<DataSnapshot>().also{it.setResult(snapshot(path.substringAfterLast('/'),reads[path],path))}.task}
            every{node.setValue(any())} answers{writes[path]=firstArg();success}
            every{node.removeValue()} answers{removed+=path;success}
            every{node.addValueEventListener(any())} answers{listeners[path]=firstArg();firstArg()}
            every{node.removeEventListener(any<ValueEventListener>())} just Runs
        }
    }
    private fun snapshot(key:String,value:Any?,path:String):DataSnapshot=mockk<DataSnapshot>().also { snap ->
        every{snap.key} returns key;every{snap.value} returns value;every{snap.exists()} returns (value!=null);every{snap.ref} answers{ref(path)}
        every{snap.children} answers{(value as? Map<*,*>)?.entries?.map{snapshot(it.key.toString(),it.value,"$path/${it.key}")} ?: emptyList()}
        every{snap.child(any())} answers{val child=firstArg<String>();snapshot(child,(value as? Map<*,*>)?.get(child),"$path/$child")}
        every{snap.getValue(String::class.java)} returns (value as? String)
    }
    @Before fun setup(){mockkStatic(FirebaseDatabase::class);every{FirebaseDatabase.getInstance()} returns database;every{database.reference} answers{ref("")};every{database.getReference(any())} answers{ref(firstArg())}}
    @After fun cleanup(){unmockkStatic(FirebaseDatabase::class)}
    private suspend fun <T> TestScope.observe(flow:Flow<T>,path:String,data:Any?):T {
        val deferred=async{flow.first()};runCurrent();val listener=requireNotNull(listeners[path]){"Listener missing: $path"}
        listener.onDataChange(snapshot(path.substringAfterLast('/'),data,path));runCurrent();val value=deferred.await()
        verify{ref(path).removeEventListener(listener)};return value
    }
    private val root="merchants/m1"
    private val tx=mapOf("id" to "41","customerId" to 7.0,"amount" to "25.50","originalAmount" to 30,"paymentType" to "CASH","isPaid" to true,"date" to "100","returnStatus" to "PARTIALLY_RETURNED","hasItems" to true)
    @Test fun cashPaymentTypeSurvivesUploadFetchAndRealtimeSnapshots()=runTest {
        val service=FirebaseSyncService();service.pushTransaction("m1",Transaction(41,7,"fixture",25.5,isPaid=true,paymentType=PaymentType.CASH))
        assertEquals("CASH",(writes["$root/transactions/41"] as Map<*,*>)["paymentType"])
        reads["$root/transactions"]=mapOf("41" to tx)
        assertEquals(PaymentType.CASH,service.fetchAllData("m1").transactions.single().paymentType)
        assertEquals(PaymentType.CASH,observe(service.observeTransactions("m1"),"$root/transactions",mapOf("41" to tx)).single().paymentType)
    }
    @Test fun numericWireFormsPreserveDebtReturnAndCustomerFieldsAndRejectMalformedRows()=runTest {
        val service=FirebaseSyncService()
        val rows=observe(service.observeTransactions("m1"),"$root/transactions",mapOf("41" to tx,"bad" to mapOf("customerId" to 7),"invalid" to "not a record"))
        assertEquals(1,rows.size);val value=rows.single();assertEquals(41,value.id);assertEquals(7,value.customerId);assertEquals(25.5,value.amount,0.0);assertEquals(30.0,value.originalAmount,0.0);assertEquals(TransactionReturnStatus.PARTIALLY_RETURNED,value.returnStatus);assertEquals(100,value.date)
        val customers=observe(service.observeCustomers("m1"),"$root/customers",mapOf("7" to mapOf("name" to "سارة","createdAt" to 1),"skip" to mapOf("id" to 9)))
        assertEquals(Customer(7,"سارة",createdAt=1),customers.single())
        val methods=observe(service.observePaymentMethods("m1"),"$root/payment_methods",mapOf("1" to mapOf("name" to "نقد","type" to "CASH"),"2" to mapOf("name" to "آخر","type" to "future")))
        assertEquals(listOf(PaymentType.CASH,PaymentType.OTHER),methods.map{it.type})
    }
    @Test fun cashSnapshotsKeepMerchantIdentityInitialBalanceAndMovementType()=runTest {
        val service=FirebaseSyncService();val box=mapOf("id" to "box","paymentMethodId" to "1","paymentMethodName" to "نقد","currentBalance" to "120.50","initialBalance" to 100,"initialBalanceSetAt" to 1,"updatedAt" to 2)
        reads["$root/cash_boxes/box"]=box;val fetched=service.fetchCashBox("m1","box")!!
        assertEquals("m1",fetched.merchantId);assertEquals(120.5,fetched.currentBalance,0.0);assertEquals(100.0,fetched.initialBalance,0.0)
        assertEquals(fetched,observe(service.observeCashBoxes("m1"),"$root/cash_boxes",mapOf("box" to box)).single())
        val rows=observe(service.observeCashBoxMovements("m1"),"$root/cash_box_movements",mapOf("move" to mapOf("cashBoxId" to "box","type" to "TRANSACTION_EFFECT","amountDelta" to 20.5,"balanceAfter" to "120.5","relatedTransactionId" to "41","createdAt" to 2),"invalid" to mapOf("id" to "bad")))
        assertEquals(1,rows.size);assertEquals(41L,rows.single().relatedTransactionId);assertEquals("m1",rows.single().merchantId);assertEquals(CashBoxMovementType.TRANSACTION_EFFECT,rows.single().type)
        service.pushCashBox("m1",fetched);service.pushCashBoxMovement("m1",rows.single())
        assertEquals(120.5,(writes["$root/cash_boxes/box"] as Map<*,*>)["currentBalance"]);assertEquals("TRANSACTION_EFFECT",(writes["$root/cash_box_movements/move"] as Map<*,*>)["type"])
    }
    @Test fun invoiceAndEmployeeSnapshotsKeepIdsAndSkipUnusableRecords()=runTest {
        val service=FirebaseSyncService()
        val item=mapOf("id" to "line","transactionId" to "41","productId" to "p","unitId" to "u","quantity" to "2.5","pricePerUnit" to 10,"totalPrice" to 25)
        val rows=observe(service.observeInvoiceItems("m1"),"$root/invoice_items",mapOf("line" to item,"invalid" to item.minus("unitId")))
        assertEquals(1,rows.size);assertEquals("m1",rows.single().merchantId);assertEquals(2.5,rows.single().quantity,0.0)
        val employees=observe(service.observeEmployees("m1"),"$root/employees",mapOf("e" to mapOf("name" to "بائع","pinCode" to "1234","role" to "future","createdAt" to "1"),"bad" to mapOf("name" to "غير صالح")))
        assertEquals(1,employees.size);assertEquals("e",employees.single().id);assertEquals(EmployeeRole.CASHIER,employees.single().role)
    }
    @Test fun activationUnknownDisabledExpiredAndTimeoutAreDistinct()=runTest {
        val service=FirebaseSyncService()
        assertEquals(ValidationResult.NotFound,service.validateCodeDetailed("missing"))
        for((wire,expected) in listOf("active" to ValidationResult.Active,"DISABLED" to ValidationResult.Disabled,"EXPIRED" to ValidationResult.Expired)) {
            reads["activation_codes/code"]=mapOf("status" to wire)
            assertEquals(expected,service.validateCodeDetailed("code"));assertEquals(wire.uppercase(),service.getCodeStatus("code"))
        }
        reads["activation_codes/code"]=mapOf("status" to "future");assertEquals("DELETED",service.getCodeStatus("code"));assertFalse(service.validateCode("code"))
        val timeoutRef=ref("activation_codes/timeout")
        every{timeoutRef.get()} returns TaskCompletionSource<DataSnapshot>().task
        assertEquals(ValidationResult.NetworkError,service.validateCodeDetailed("timeout"))
    }
    @Test fun currentSessionListenerClosesOnPermissionFailureAndUnregisters()=runTest {
        val service=FirebaseSyncService();val errors=mutableListOf<Throwable>()
        val collection=service.observeCurrentSession("m1","s").catch{errors+=it}.launchIn(backgroundScope);runCurrent()
        listeners["$root/sessions/s"]!!.onCancelled(DatabaseError.fromCode(DatabaseError.PERMISSION_DENIED));runCurrent()
        assertEquals(1,errors.size);verify{ref("$root/sessions/s").removeEventListener(any<ValueEventListener>())};assertTrue(collection.isCompleted)
    }
    @Test fun uploadAndDeleteUseOnlyRequestedMerchantAndPreserveInvoiceAmounts()=runTest {
        val service=FirebaseSyncService();val customer=Customer(7,"fixture",phone="0500000000",createdAt=1)
        service.pushCustomer("m1",customer);service.pushPaymentMethod("m1",PaymentMethod(2,"نقد",PaymentType.CASH));service.pushInvoiceItems("m1",listOf(InvoiceItem("line",41,"p","أرز","u","كيس",2.0,12.5,25.0,"m1")))
        assertEquals(7L,(writes["$root/customers/7"] as Map<*,*>)["id"]);assertEquals("CASH",(writes["$root/payment_methods/2"] as Map<*,*>)["type"])
        assertEquals(25.0,(writes["$root/invoice_items/line"] as Map<*,*>)["totalPrice"]);assertTrue(writes.keys.all{it.startsWith("$root/")})
        service.deleteCustomer("m1",7);service.deletePaymentMethod("m1",2);service.deleteTransaction("m1",41);service.deleteCashBox("m1","box")
        assertEquals(setOf("$root/customers/7","$root/payment_methods/2","$root/transactions/41","$root/cash_boxes/box"),removed)
    }
    @Test fun bulkAndRealtimeReturnSnapshotsPreserveLegacyUnitFallbackAndRefunds()=runTest {
        val invoice=mapOf("id" to "line","transactionId" to 41L,"productId" to "p","productName" to "أرز","unitId" to "u","unitLabel" to "كيس","quantity" to 3,"pricePerUnit" to 20,"totalPrice" to 60)
        val returned=mapOf("id" to "ri","productId" to "p","returnedQty" to "1.0","costPricePerUnit" to 12,"lostProfit" to "8.0","totalRefund" to 20)
        val parent=mapOf("originalTransactionId" to "41","returnType" to "PARTIAL","totalRefund" to "20.0","createdAt" to 1L)
        val returns=mapOf("r-map" to parent.plus("items" to mapOf("ri" to returned)),"r-list" to parent.plus("items" to listOf(returned)))
        reads["$root/customers"]=mapOf("7" to mapOf("name" to "fixture","createdAt" to 1))
        reads["$root/payment_methods"]=mapOf("1" to mapOf("name" to "نقد","type" to "CASH"))
        reads["$root/transactions"]=mapOf("41" to tx);reads["$root/invoice_items"]=mapOf("line" to invoice);reads["$root/return_invoices"]=returns
        reads["$root/cash_boxes"]=mapOf("1" to mapOf("currentBalance" to 120,"initialBalance" to 100,"updatedAt" to 1))
        reads["$root/cash_box_movements"]=mapOf("m" to mapOf("cashBoxId" to "1","amountDelta" to 20,"balanceAfter" to 120,"createdAt" to 1))
        val service=FirebaseSyncService();val bulk=service.fetchAllData("m1")
        assertEquals(1,bulk.customers.size);assertEquals(1,bulk.transactions.size);assertEquals(1,bulk.paymentMethods.size);assertEquals(1,bulk.invoiceItems.size)
        assertEquals(1,bulk.cashBoxes.size);assertEquals(1,bulk.cashBoxMovements.size)
        for(rows in listOf(bulk.returns,observe(service.observeReturnInvoices("m1"),"$root/return_invoices",returns))) {
            assertEquals(2,rows.size)
            for((parent,items) in rows) {
                assertEquals(41,parent.originalTransactionId);assertEquals("m1",parent.merchantId);val line=items.single()
                assertEquals("u",line.unitId);assertEquals("أرز",line.productName);assertEquals(3.0,line.originalQuantity,0.0);assertEquals(1.0,line.returnedQuantity,0.0);assertEquals(20.0,line.totalRefund,0.0);assertEquals(8.0,line.lostProfit,0.0)
            }
        }
    }
    @Test fun legacyTransactionsWithoutPaymentTypeKeepExistingDebtDefault()=runTest {
        val service=FirebaseSyncService();val legacy=tx.minus("paymentType")
        reads["$root/transactions"]=mapOf("41" to legacy)
        assertEquals(PaymentType.DEBT,service.fetchAllData("m1").transactions.single().paymentType)
        assertEquals(PaymentType.DEBT,observe(service.observeTransactions("m1"),"$root/transactions",mapOf("41" to legacy)).single().paymentType)
    }

    private fun <T> TestScope.assertPermissionFailure(flow:Flow<T>,path:String) {
        val errors=mutableListOf<Throwable>();val values=mutableListOf<T>()
        val job=flow.onEach{values+=it}.catch{errors+=it}.launchIn(backgroundScope);runCurrent()
        val listener=requireNotNull(listeners[path]);listener.onCancelled(DatabaseError.fromCode(DatabaseError.PERMISSION_DENIED));runCurrent()
        assertEquals("Terminal SDK errors must close this stream explicitly",1,errors.size)
        assertTrue("An error must never masquerade as an empty remote snapshot",values.isEmpty())
        assertTrue(job.isCompleted);verify(exactly=1){ref(path).removeEventListener(listener)}
    }
    @Test fun customersPermissionFailureClosesAndUnregisters()=runTest {
        assertPermissionFailure(FirebaseSyncService().observeCustomers("m1"),"$root/customers")
    }
    @Test fun transactionsPermissionFailureClosesWithoutAnEmptySnapshot()=runTest {
        assertPermissionFailure(FirebaseSyncService().observeTransactions("m1"),"$root/transactions")
    }
    @Test fun paymentMethodsPermissionFailureClosesAndUnregisters()=runTest {
        assertPermissionFailure(FirebaseSyncService().observePaymentMethods("m1"),"$root/payment_methods")
    }
    @Test fun cashBoxesPermissionFailureClosesWithoutChangingBalances()=runTest {
        assertPermissionFailure(FirebaseSyncService().observeCashBoxes("m1"),"$root/cash_boxes")
    }
    @Test fun cashMovementsPermissionFailureClosesWithoutAnEmptyLedger()=runTest {
        assertPermissionFailure(FirebaseSyncService().observeCashBoxMovements("m1"),"$root/cash_box_movements")
    }
    @Test fun invoiceItemsPermissionFailureClosesWithoutDroppingInvoiceLines()=runTest {
        assertPermissionFailure(FirebaseSyncService().observeInvoiceItems("m1"),"$root/invoice_items")
    }
    @Test fun returnInvoicesPermissionFailureClosesWithoutDeletingReturns()=runTest {
        assertPermissionFailure(FirebaseSyncService().observeReturnInvoices("m1"),"$root/return_invoices")
    }
    @Test fun employeesPermissionFailureClosesAndUnregisters()=runTest {
        assertPermissionFailure(FirebaseSyncService().observeEmployees("m1"),"$root/employees")
    }

    @Test fun cancellingEverySellerListenerRemovesItAndLateCallbacksCannotEmit()=runTest {
        val service=FirebaseSyncService()
        val families=listOf("customers" to service.observeCustomers("m1"),"transactions" to service.observeTransactions("m1"),
            "payment_methods" to service.observePaymentMethods("m1"),"cash_boxes" to service.observeCashBoxes("m1"),
            "cash_box_movements" to service.observeCashBoxMovements("m1"),"invoice_items" to service.observeInvoiceItems("m1"),
            "return_invoices" to service.observeReturnInvoices("m1"),"employees" to service.observeEmployees("m1"))
        for((family,flow) in families) {
            val values=mutableListOf<Any?>();val path="$root/$family"
            val job=flow.onEach{values.add(it)}.launchIn(backgroundScope);runCurrent()
            val listener=requireNotNull(listeners[path]);job.cancelAndJoin()
            verify(exactly=1){ref(path).removeEventListener(listener)}
            listener.onDataChange(snapshot(family,emptyMap<String,Any>(),path));runCurrent()
            assertTrue("Late $family callbacks must not reach cancelled collectors",values.isEmpty())
        }
    }

    @Test fun customerSnapshotsRetainSupportedIdsAndSkipMalformedRecordsOnBothReadPaths()=runTest {
        val rows=linkedMapOf<String,Any>(
            "explicit" to mapOf("id" to 7L,"name" to "سارة","phone" to true,"createdAt" to "invalid"),
            "8" to mapOf("id" to false,"name" to "ليلى","createdAt" to 9L),
            "bad-key" to mapOf("id" to false,"name" to "skip"),
            "missing-name" to mapOf("id" to 10L),"primitive" to "invalid")
        reads["$root/customers"]=rows
        val service=FirebaseSyncService()
        for(values in listOf(service.fetchAllData("m1").customers,observe(service.observeCustomers("m1"),"$root/customers",rows))){
            assertEquals(setOf(7L,8L),values.map{it.id}.toSet())
            assertEquals("",values.single{it.id==7L}.phone);assertTrue(values.single{it.id==7L}.createdAt>0)
            assertEquals(9L,values.single{it.id==8L}.createdAt)
        }
    }
    @Test fun paymentSchemasPreserveIntegerDecimalAndStringIdsWithDocumentedTypeFallback()=runTest {
        val rows=linkedMapOf<String,Any>(
            "a" to mapOf("id" to 1L,"name" to "cash","type" to "CASH"),
            "b" to mapOf("id" to 2.0,"name" to "bank","type" to "BANK"),
            "3" to mapOf("id" to "invalid","name" to "fallback","type" to false),
            "d" to mapOf("id" to "4","name" to "future","type" to "future"),
            "bad" to mapOf("id" to "invalid","name" to "skip"),"missing" to mapOf("id" to 9L),"primitive" to true)
        reads["$root/payment_methods"]=rows;val service=FirebaseSyncService()
        for(values in listOf(service.fetchAllData("m1").paymentMethods,observe(service.observePaymentMethods("m1"),"$root/payment_methods",rows))){
            assertEquals(setOf(1L,2L,3L,4L),values.map{it.id}.toSet())
            assertEquals(PaymentType.CASH,values.single{it.id==1L}.type);assertEquals(PaymentType.BANK,values.single{it.id==2L}.type)
            assertTrue(values.filter{it.id>=3L}.all{it.type==PaymentType.OTHER})
        }
    }
    @Test fun transactionOptionalWireFieldsFallBackWithoutAcceptingMissingRequiredIdentityOrAmount()=runTest {
        val rows=linkedMapOf<String,Any>(
            "41" to mapOf("id" to false,"customerId" to 7L,"amount" to 25.0),
            "42" to mapOf("id" to 42L,"customerId" to "7","amount" to 30L,"originalAmount" to false,"returnStatus" to "future","paymentType" to "future","isPaid" to "true","paymentMethodId" to 2.0,"note" to false,"date" to 400L,"paidAt" to "300","hasItems" to 0L,"dueDate" to 500L,"reminderEnabled" to false),
            "43" to mapOf("customerId" to 7L),"44" to mapOf("amount" to 9.0),
            "bad-key" to mapOf("customerId" to 7L,"amount" to 9.0),"primitive" to "invalid")
        reads["$root/transactions"]=rows;val service=FirebaseSyncService()
        for(values in listOf(service.fetchAllData("m1").transactions,observe(service.observeTransactions("m1"),"$root/transactions",rows))){
            assertEquals(setOf(41L,42L),values.map{it.id}.toSet())
            val first=values.single{it.id==41L};assertEquals(25.0,first.originalAmount,0.0);assertTrue(first.date>0);assertTrue(first.reminderEnabled)
            val second=values.single{it.id==42L};assertEquals(30.0,second.originalAmount,0.0);assertEquals(TransactionReturnStatus.NONE,second.returnStatus)
            assertEquals(PaymentType.DEBT,second.paymentType);assertFalse(second.isPaid);assertFalse(second.hasItems);assertFalse(second.reminderEnabled)
            assertEquals(2L,second.paymentMethodId);assertEquals(300L,second.paidAt);assertEquals(500L,second.dueDate);assertEquals(400L,second.date);assertEquals("",second.note)
        }
    }
    @Test fun invoiceReadPathsKeepTheirExplicitSparseLegacyAndRealtimeRequirements()=runTest {
        val complete=mapOf("id" to "live","transactionId" to 41L,"productId" to "p","unitId" to "u","quantity" to "2.5","pricePerUnit" to 10L,"totalPrice" to 25.0,"merchantId" to "other")
        val rows=linkedMapOf<String,Any>("live" to complete,"legacy" to mapOf("id" to "legacy","transactionId" to "41"),"missing-id" to mapOf("transactionId" to 41L),"missing-tx" to complete.minus("transactionId"),"primitive" to false)
        reads["$root/invoice_items"]=rows;val service=FirebaseSyncService()
        val fetched=service.fetchAllData("m1").invoiceItems
        assertEquals(setOf("live","legacy"),fetched.map{it.id}.toSet())
        val sparse=fetched.single{it.id=="legacy"};assertEquals("",sparse.productId);assertEquals("",sparse.unitId);assertEquals(0.0,sparse.quantity,0.0)
        val live=observe(service.observeInvoiceItems("m1"),"$root/invoice_items",rows).single()
        assertEquals("live",live.id);assertEquals(2.5,live.quantity,0.0);assertEquals(10.0,live.pricePerUnit,0.0);assertEquals(25.0,live.totalPrice,0.0)
        assertTrue(fetched.all{it.merchantId=="m1"});assertEquals("m1",live.merchantId)
    }
    @Test fun cashBoxWireFallbacksKeepTheRequestedMerchantAndSkipUnusableMethodIds()=runTest {
        val rows=linkedMapOf<String,Any>(
            "1" to mapOf("id" to false,"paymentMethodId" to false,"paymentMethodName" to true,"currentBalance" to false),
            "box" to mapOf("id" to "explicit-box","paymentMethodId" to "2","paymentMethodName" to "bank","currentBalance" to 120L,"initialBalance" to "100.5","initialBalanceSetAt" to 3.0,"updatedAt" to "4","merchantId" to "other"),
            "bad-key" to mapOf("id" to "bad"),"primitive" to "invalid")
        reads["$root/cash_boxes"]=rows;val service=FirebaseSyncService()
        for(values in listOf(service.fetchAllData("m1").cashBoxes,observe(service.observeCashBoxes("m1"),"$root/cash_boxes",rows))){
            assertEquals(setOf("1","explicit-box"),values.map{it.id}.toSet())
            val fallback=values.single{it.id=="1"};assertEquals(1L,fallback.paymentMethodId);assertEquals(0.0,fallback.currentBalance,0.0);assertNull(fallback.initialBalanceSetAt);assertTrue(fallback.updatedAt>0)
            val explicit=values.single{it.id=="explicit-box"};assertEquals(120.0,explicit.currentBalance,0.0);assertEquals(100.5,explicit.initialBalance,0.0);assertEquals(3L,explicit.initialBalanceSetAt);assertEquals(4L,explicit.updatedAt)
            assertTrue(values.all{it.merchantId=="m1"&&it.syncStatus==SyncStatus.SYNCED})
        }
    }
    @Test fun cashMovementWireFallbacksRetainSignedAmountsAndRelatedTransaction()=runTest {
        val rows=linkedMapOf<String,Any>(
            "move" to mapOf("id" to false,"cashBoxId" to "box","type" to false,"paymentMethodName" to false,"amountDelta" to "-12.5","balanceAfter" to false,"note" to false,"relatedTransactionId" to 41.0,"createdAt" to "2"),
            "defaults" to mapOf("cashBoxId" to "box"),"missing-box" to mapOf("id" to "skip"),"primitive" to false)
        reads["$root/cash_box_movements"]=rows;val service=FirebaseSyncService()
        for(values in listOf(service.fetchAllData("m1").cashBoxMovements,observe(service.observeCashBoxMovements("m1"),"$root/cash_box_movements",rows))){
            assertEquals(setOf("move","defaults"),values.map{it.id}.toSet())
            val movement=values.single{it.id=="move"};assertEquals(-12.5,movement.amountDelta,0.0);assertEquals(0.0,movement.balanceAfter,0.0);assertEquals(41L,movement.relatedTransactionId);assertEquals(2L,movement.createdAt)
            assertEquals(CashBoxMovementType.MANUAL_ADJUSTMENT,movement.type);assertEquals("",movement.note);assertEquals("",movement.paymentMethodName)
            val defaults=values.single{it.id=="defaults"};assertNull(defaults.relatedTransactionId);assertTrue(defaults.createdAt>0)
            assertTrue(values.all{it.merchantId=="m1"&&it.syncStatus==SyncStatus.SYNCED})
        }
    }
    @Test fun returnWireMapAndListItemsKeepLegacyBindingsAndExplicitUnitOverrides()=runTest {
        reads["$root/invoice_items"]=mapOf("sold" to mapOf("id" to "sold","transactionId" to 41L,"productId" to "p","productName" to "أرز","unitId" to "u","unitLabel" to "كيس","quantity" to 5L,"pricePerUnit" to 10L))
        val rows=linkedMapOf<String,Any>(
            "map-return" to mapOf("transactionId" to "41","returnType" to "future","totalRefund" to "15","note" to false,"items" to mapOf("legacy" to mapOf("productId" to "p","returnedQty" to "1.5","costPricePerUnit" to 2L,"lostProfit" to 3L,"totalRefund" to 15L),"skip" to true)),
            "list-return" to mapOf("originalTransactionId" to 41.0,"returnType" to "FULL","createdAt" to 2L,"items" to listOf(null,"skip",mapOf("id" to "explicit","productId" to "other","unitId" to "x","originalQuantity" to 2L,"returnedQuantity" to 1.0,"pricePerUnit" to 5L),mapOf("productId" to "unmatched"))),
            "empty-return" to mapOf("originalTransactionId" to 41L),"missing-tx" to mapOf("id" to "skip"),"primitive" to false)
        reads["$root/return_invoices"]=rows;val service=FirebaseSyncService()
        for(values in listOf(service.fetchAllData("m1").returns,observe(service.observeReturnInvoices("m1"),"$root/return_invoices",rows))){
            assertEquals(setOf("map-return","list-return","empty-return"),values.map{it.first.id}.toSet())
            val legacy=values.single{it.first.id=="map-return"};assertEquals(ReturnType.PARTIAL,legacy.first.returnType);assertEquals(15.0,legacy.first.totalRefund,0.0);assertTrue(legacy.first.createdAt>0)
            val item=legacy.second.single();assertTrue(item.id.isNotBlank());assertEquals("u",item.unitId);assertEquals("أرز",item.productName);assertEquals("كيس",item.unitLabel);assertEquals(5.0,item.originalQuantity,0.0);assertEquals(1.5,item.returnedQuantity,0.0);assertEquals(10.0,item.pricePerUnit,0.0);assertEquals(2.0,item.costPricePerUnit,0.0);assertEquals(3.0,item.lostProfit,0.0)
            val explicit=values.single{it.first.id=="list-return"};assertEquals(ReturnType.FULL,explicit.first.returnType);assertEquals(2L,explicit.first.createdAt);assertEquals("explicit",explicit.second.single().id);assertEquals("x",explicit.second.single().unitId);assertEquals(2.0,explicit.second.single().originalQuantity,0.0);assertEquals(1.0,explicit.second.single().returnedQuantity,0.0)
            assertTrue(values.single{it.first.id=="empty-return"}.second.isEmpty());assertTrue(values.all{it.first.merchantId=="m1"})
        }
    }
    @Test fun employeeWireRequiresNameAndPinAndPreservesSupportedRolesAndMerchant()=runTest {
        val rows=linkedMapOf<String,Any>(
            "cashier" to mapOf("id" to false,"name" to "بائع","pinCode" to "1234","role" to false),
            "admin" to mapOf("id" to "owner-employee","name" to "مدير","pinCode" to "5678","role" to "ADMIN","createdAt" to "2","merchantId" to "other"),
            "missing-name" to mapOf("pinCode" to "0000"),"missing-pin" to mapOf("name" to "skip"),"primitive" to true)
        val values=observe(FirebaseSyncService().observeEmployees("m1"),"$root/employees",rows)
        assertEquals(setOf("cashier","owner-employee"),values.map{it.id}.toSet())
        assertEquals(EmployeeRole.CASHIER,values.single{it.id=="cashier"}.role);assertTrue(values.single{it.id=="cashier"}.createdAt>0)
        assertEquals(EmployeeRole.ADMIN,values.single{it.id=="owner-employee"}.role);assertEquals(2L,values.single{it.id=="owner-employee"}.createdAt)
        assertTrue(values.all{it.merchantId=="m1"})
    }
    @Test fun subscriptionStatusUsesTheProvidedLatestChildAndDoesNotApproveUnknownStatus()=runTest {
        val service=FirebaseSyncService();val path="subscription_requests/m1"
        assertEquals(SubscriptionStatus.PENDING,observe(service.observeSubscriptionRequestStatus("m1"),path,emptyMap<String,Any>()))
        assertEquals(SubscriptionStatus.PENDING,observe(service.observeSubscriptionRequestStatus("m1"),path,linkedMapOf("old" to mapOf("status" to "APPROVED"),"latest" to mapOf("status" to "future"))))
        assertEquals(SubscriptionStatus.APPROVED,observe(service.observeSubscriptionRequestStatus("m1"),path,mapOf("latest" to mapOf("status" to "APPROVED"))))
    }
    @Test fun cancellingEachSnapshotReadNeverReturnsAnAuthoritativePayloadToItsConsumer()=runTest {
        val service=FirebaseSyncService();val returnedAfterCancellation=mutableListOf<String>()
        for(family in listOf("customers","payment_methods","transactions","invoice_items","return_invoices","cash_boxes","cash_box_movements")){
            val path="$root/$family";val node=ref(path);val gate=TaskCompletionSource<DataSnapshot>()
            every{node.get()} returns gate.task
            var consumed=false
            val job=launch{service.fetchAllData("m1");consumed=true}
            runCurrent();assertFalse("Read must actually suspend at $family",job.isCompleted)
            job.cancelAndJoin();if(consumed)returnedAfterCancellation+=family
            gate.setResult(snapshot(family,emptyMap<String,Any>(),path));runCurrent()
            every{node.get()} answers{TaskCompletionSource<DataSnapshot>().also{it.setResult(snapshot(family,reads[path],path))}.task}
        }
        assertEquals("Cancelled snapshots must not become authoritative empty/partial data",emptyList<String>(),returnedAfterCancellation)
    }
    @Test fun cancelledActivationValidationNeverReturnsANetworkErrorToTheConsumer()=runTest {
        val node=ref("activation_codes/pending");val gate=TaskCompletionSource<DataSnapshot>();every{node.get()} returns gate.task
        var consumed=false;val job=launch{FirebaseSyncService().validateCodeDetailed("pending");consumed=true}
        runCurrent();assertFalse(job.isCompleted);job.cancelAndJoin()
        gate.setResult(snapshot("pending",mapOf("status" to "ACTIVE"),"activation_codes/pending"));runCurrent()
        assertFalse("Parent cancellation must escape validation",consumed)
    }
    @Test fun cancelledCodeStatusReadNeverReturnsAFalseMissingStatusToTheConsumer()=runTest {
        val node=ref("activation_codes/pending");val gate=TaskCompletionSource<DataSnapshot>();every{node.get()} returns gate.task
        var consumed=false;val job=launch{FirebaseSyncService().getCodeStatus("pending");consumed=true}
        runCurrent();assertFalse(job.isCompleted);job.cancelAndJoin()
        gate.setResult(snapshot("pending",mapOf("status" to "ACTIVE"),"activation_codes/pending"));runCurrent()
        assertFalse("Parent cancellation must escape code-status lookup",consumed)
    }

    @Test fun cachedSnapshotCompletionStillHonorsCancellationBeforeDeliveringData()=runTest {
        val service=FirebaseSyncService();val path="$root/customers";val node=ref(path)
        lateinit var job:Job
        every{node.get()} answers {
            job.cancel()
            TaskCompletionSource<DataSnapshot>().also{it.setResult(snapshot("customers",emptyMap<String,Any>(),path))}.task
        }
        var consumed=false
        job=launch(start=CoroutineStart.LAZY){service.fetchAllData("m1");consumed=true}
        job.start();runCurrent();job.join()
        assertFalse("Cached task completion must not bypass the cancelled caller",consumed)
    }
    @Test fun cancellationDuringFinalSnapshotParsingCannotDeliverThePayload()=runTest {
        val service=FirebaseSyncService();val path="$root/cash_box_movements"
        val payload=mapOf("cashBoxId" to "box","amountDelta" to 1.0)
        val item=snapshot("movement",payload,"$path/movement")
        val parent=snapshot("cash_box_movements",emptyMap<String,Any>(),path)
        lateinit var job:Job
        every{parent.children} returns listOf(item)
        every{item.value} answers{job.cancel();payload}
        val node=ref(path)
        every{node.get()} returns TaskCompletionSource<DataSnapshot>().also{it.setResult(parent)}.task
        var consumed=false
        job=launch(start=CoroutineStart.LAZY){service.fetchAllData("m1");consumed=true}
        job.start();runCurrent();job.join()
        assertFalse("Cancellation during the final family must escape before snapshot delivery",consumed)
    }

}
