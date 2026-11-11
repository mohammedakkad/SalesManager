package com.trader.core.data.remote

import android.app.Application
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.trader.core.domain.model.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.io.IOException

/** Production Firestore wire code against deterministic SDK tasks; never contacts a Firebase project. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],manifest=Config.NONE,application=Application::class)
class ProductFirestoreContractsTest {
    private val db=mockk<FirebaseFirestore>()
    private val collections=mutableMapOf<String,CollectionReference>()
    private val documents=mutableMapOf<String,DocumentReference>()
    private val reads=mutableMapOf<String,List<Pair<String,Map<String,Any?>?>>>()
    private val writes=mutableMapOf<String,Any>()
    private val deletes=mutableListOf<String>()
    private val listeners=mutableMapOf<String,EventListener<QuerySnapshot>>()
    private val removed=mutableSetOf<String>()
    private val batch=mockk<WriteBatch>(relaxed=true)
    private fun collection(path:String):CollectionReference=collections.getOrPut(path){
        mockk<CollectionReference>(relaxed=true).also{node->
            every{node.document(any())} answers {document("$path/${firstArg<String>()}")}
            every{node.get()} answers {Tasks.forResult(snapshot(path))}
            every{node.addSnapshotListener(any<EventListener<QuerySnapshot>>())} answers {
                listeners[path]=firstArg();mockk<ListenerRegistration>{every{remove()} answers {removed+=path;Unit}}
            }
        }
    }
    private fun document(path:String):DocumentReference=documents.getOrPut(path){
        mockk<DocumentReference>(relaxed=true).also{node->
            every{node.path} returns path;every{node.id} returns path.substringAfterLast('/')
            every{node.collection(any())} answers {collection("$path/${firstArg<String>()}")}
            every{node.set(any<Any>())} answers {writes[path]=firstArg();Tasks.forResult(null)}
            every{node.delete()} answers {deletes+=path;Tasks.forResult(null)}
            every{node.get()} answers {Tasks.forResult(doc(path,reads[path]?.singleOrNull()?.second))}
            every{node.update(any<String>(),any(),*anyVararg())} answers {writes[path]=args;Tasks.forResult(null)}
        }
    }
    private fun doc(path:String,data:Map<String,Any?>?):DocumentSnapshot=mockk<DocumentSnapshot>().also {s->
        every{s.id} returns path.substringAfterLast('/');every{s.data} returns data;every{s.reference} returns document(path)
        every{s.getDouble(any())} answers {(data?.get(firstArg<String>()) as? Number)?.toDouble()}
    }
    private fun snapshot(path:String)=mockk<QuerySnapshot>().also{s->every{s.documents} answers {reads[path].orEmpty().map{(id,data)->doc("$path/$id",data)}}}
    @Before fun setup(){
        mockkStatic(FirebaseFirestore::class);every{FirebaseFirestore.getInstance()} returns db
        every{db.collection(any())} answers {collection(firstArg())};every{db.batch()} returns batch
        every{batch.set(any<DocumentReference>(),any<Any>())} answers {writes[firstArg<DocumentReference>().path]=secondArg();batch}
        every{batch.delete(any())} answers {deletes+=firstArg<DocumentReference>().path;batch}
        every{batch.commit()} returns Tasks.forResult(null)
    }
    @After fun cleanup(){unmockkStatic(FirebaseFirestore::class)}
    private val root="merchants/m1"
    private val units="$root/products/p/units"
    private val unit=ProductUnit("u","p",UnitType.WEIGHT,"kg",price=25.5,costPrice=12.25,quantityInStock=3.75,
        itemsPerCarton=6,lowStockThreshold=1.5,isDefault=true,weightUnit=WeightUnit.OZ,createdAt=2,updatedAt=3)
    private val wire=mapOf<String,Any?>("productId" to "p","unitType" to "WEIGHT","unitLabel" to "kg","price" to 25.5,
        "costPrice" to 12.25,"quantityInStock" to 3.75,"itemsPerCarton" to 6L,"lowStockThreshold" to 1.5,
        "isDefault" to true,"weightUnit" to "OZ","createdAt" to 2L,"updatedAt" to 3L)
    @Test fun uploadUnitPreservesCostAlongsidePriceStockAndWeight()=runTest {
        ProductFirestoreService().uploadUnit("m1",unit)
        val value=writes["$units/u"] as Map<*,*>
        assertEquals(12.25,value["costPrice"]);assertEquals(25.5,value["price"]);assertEquals("OZ",value["weightUnit"])
        assertEquals(3.75,value["quantityInStock"]);assertEquals("SYNCED",value["syncStatus"])
    }
    @Test fun fetchedUnitPreservesCostAndAllPhysicalUnitFields()=runTest {
        reads[units]=listOf("u" to wire)
        assertEquals(unit.copy(syncStatus=SyncStatus.SYNCED),ProductFirestoreService().fetchUnitsForProduct("m1","p").single())
    }
    @Test fun integerWireAmountsDoNotSilentlyBecomeZero()=runTest {
        reads[units]=listOf("u" to wire.plus(mapOf("price" to 25L,"costPrice" to 12L,"quantityInStock" to 3L,"lowStockThreshold" to 1L)))
        val row=ProductFirestoreService().fetchUnitsForProduct("m1","p").single()
        assertEquals(25.0,row.price,0.0);assertEquals(12.0,row.costPrice,0.0);assertEquals(3.0,row.quantityInStock,0.0);assertEquals(1.0,row.lowStockThreshold,0.0)
    }
    @Test fun failedCatalogReadCannotMasqueradeAsAnEmptyCatalog()=runTest {
        val failure=IOException("offline fixture");val node=collection("$root/products");every{node.get()} returns Tasks.forException(failure)
        try{ProductFirestoreService().fetchAllProducts("m1");fail("Failed read reported success")}catch(actual:IOException){assertSame(failure,actual)}
    }
    @Test fun failedUnitReadCannotMasqueradeAsAProductWithNoUnits()=runTest {
        val failure=IOException("offline fixture");val node=collection(units);every{node.get()} returns Tasks.forException(failure)
        try{ProductFirestoreService().fetchUnitsForProduct("m1","p");fail("Failed read reported success")}catch(actual:IOException){assertSame(failure,actual)}
    }
    @Test fun cancelledCatalogReadRemainsCancellation()=runTest {
        val node=collection("$root/products");every{node.get()} answers {throw CancellationException("cancel fixture")}
        try{ProductFirestoreService().fetchAllProducts("m1");fail("Cancellation swallowed")}catch(expected:CancellationException){}
    }
    @Test fun cancelledUnitReadRemainsCancellation()=runTest {
        val node=collection(units);every{node.get()} answers {throw CancellationException("cancel fixture")}
        try{ProductFirestoreService().fetchUnitsForProduct("m1","p");fail("Cancellation swallowed")}catch(expected:CancellationException){}
    }
    @Test fun productListenerPermissionErrorClosesAndRemovesItsRegistration()=runTest {
        val errors=mutableListOf<Throwable>();val values=mutableListOf<List<Product>>()
        val job=ProductFirestoreService().observeProducts("m1").onEach{values+=it}.catch{errors+=it}.launchIn(backgroundScope);runCurrent()
        val failure=FirebaseFirestoreException("offline denied",FirebaseFirestoreException.Code.PERMISSION_DENIED)
        listeners["$root/products"]!!.onEvent(null,failure);runCurrent()
        assertEquals(listOf(failure),errors);assertTrue(values.isEmpty());assertTrue(job.isCompleted);assertTrue("$root/products" in removed)
    }
    @Test fun legitimateEmptyReadIsStillAnEmptySuccess()=runTest {
        val service=ProductFirestoreService();assertTrue(service.fetchAllProducts("m1").isEmpty());assertTrue(service.fetchUnitsForProduct("m1","p").isEmpty())
    }
    @Test fun productFieldsAndLegacyMissingFieldsSurviveFetchAndRealtime()=runTest {
        val good=mapOf<String,Any?>("name" to "rice","barcode" to "123","category" to "food","imageUri" to "offline://image","createdAt" to 2L,"updatedAt" to 3L)
        reads["$root/products"]=listOf("p" to good,"missing-name" to emptyMap(),"missing-data" to null)
        val service=ProductFirestoreService();val fetched=service.fetchAllProducts("m1").single()
        assertEquals("p",fetched.id);assertEquals("m1",fetched.merchantId);assertEquals("rice",fetched.name);assertEquals("123",fetched.barcode)
        val result=async{service.observeProducts("m1").first()};runCurrent();listeners["$root/products"]!!.onEvent(snapshot("$root/products"),null);runCurrent()
        assertEquals(listOf(fetched),result.await());assertTrue("$root/products" in removed)
    }
    @Test fun absentCostAndUnknownEnumsKeepLegacyDefaults()=runTest {
        reads[units]=listOf("u" to mapOf("productId" to "p","unitType" to "future","weightUnit" to "future"),"bad" to emptyMap(),"null" to null)
        val row=ProductFirestoreService().fetchUnitsForProduct("m1","p").single()
        assertEquals(UnitType.PIECE,row.unitType);assertEquals(WeightUnit.KG,row.weightUnit);assertEquals(0.0,row.costPrice,0.0)
        assertEquals(0.0,row.price,0.0);assertEquals(0.0,row.quantityInStock,0.0);assertFalse(row.isDefault);assertNull(row.itemsPerCarton)
    }
    @Test fun deletionCommitsUnitBatchBeforeDeletingItsParent()=runTest {
        reads[units]=listOf("u" to wire,"v" to wire)
        ProductFirestoreService().deleteProduct("m1","p")
        assertEquals(listOf("$units/u","$units/v","$root/products/p"),deletes)
        verifyOrder{batch.commit();document("$root/products/p").delete()}
    }
    @Test fun failedDeletionBatchCannotDeleteTheParent()=runTest {
        reads[units]=listOf("u" to wire);every{batch.commit()} returns Tasks.forException(IOException("offline fixture"))
        try{ProductFirestoreService().deleteProduct("m1","p");fail("Failure swallowed")}catch(expected:IOException){}
        assertFalse("$root/products/p" in deletes)
    }
    @Test fun deletingAProductWithNoUnitsSkipsTheBatch()=runTest {
        ProductFirestoreService().deleteProduct("m1","p");assertEquals(listOf("$root/products/p"),deletes);verify(exactly=0){db.batch()}
    }
    @Test fun unitDeletionAndRemoteStockUseOnlyTheRequestedMerchantPath()=runTest {
        val service=ProductFirestoreService();reads["$units/u"]=listOf("u" to mapOf("quantityInStock" to 7.5))
        assertEquals(7.5,service.getRemoteQuantity("m1","u","p")!!,0.0)
        service.updateRemoteQuantity("m1","u",4.25,"p");service.deleteUnit("m1","u","p")
        assertTrue(writes.containsKey("$units/u"));assertEquals(listOf("$units/u"),deletes)
    }
    @Test fun invoiceBatchPreservesAmountsAndEmptyBatchDoesNotDispatch()=runTest {
        val service=ProductFirestoreService();service.uploadInvoiceItems("m1",emptyList());verify(exactly=0){db.batch()}
        service.uploadInvoiceItems("m1",listOf(InvoiceItem("line",41,"p","rice","u","kg",2.0,25.5,51.0,"m1")))
        val value=writes["$root/invoice_items/line"] as Map<*,*>
        assertEquals(51.0,value["totalPrice"]);assertEquals(41L,value["transactionId"]);assertEquals("m1",value["merchantId"])
    }
    @Test fun productAndLedgerUploadsPreserveStableIdentities()=runTest {
        val service=ProductFirestoreService();service.uploadProduct("m1",Product("p",name="rice",merchantId="m1",createdAt=2,updatedAt=3))
        service.uploadMovement("m1",StockMovement("move","p","rice","u","kg",MovementType.MANUAL_IN,2.0,3.0,5.0,41,"note","m1",4))
        val value=writes["$root/stock_movements/move"] as Map<*,*>;assertEquals(2.0,value["quantity"]);assertEquals(41L,value["relatedTransactionId"])
        assertEquals("rice",(writes["$root/products/p"] as Map<*,*>)["name"])
    }
    private suspend fun rejectMalformedNumber(field:String,value:Any) {
        reads[units]=listOf("valid" to wire,"invalid" to wire.plus(field to value))
        try{ProductFirestoreService().fetchUnitsForProduct("m1","p");fail("Malformed $field became a successful partial snapshot")}catch(expected:IllegalArgumentException){}
    }
    @Test fun nonfiniteCostRejectsTheWholeUnitSnapshot()=runTest {rejectMalformedNumber("costPrice",Double.NaN)}
    @Test fun infinitePriceRejectsTheWholeUnitSnapshot()=runTest {rejectMalformedNumber("price",Double.POSITIVE_INFINITY)}
    @Test fun infiniteStockRejectsTheWholeUnitSnapshot()=runTest {rejectMalformedNumber("quantityInStock",Double.NEGATIVE_INFINITY)}
    @Test fun nonfiniteThresholdRejectsTheWholeUnitSnapshot()=runTest {rejectMalformedNumber("lowStockThreshold",Double.NaN)}
    @Test fun malformedNumericStringCannotBecomeAnImplicitZeroPrice()=runTest {rejectMalformedNumber("price","not-a-number")}

}
