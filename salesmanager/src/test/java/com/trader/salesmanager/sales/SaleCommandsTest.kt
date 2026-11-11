package com.trader.salesmanager.sales

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.*
import kotlinx.coroutines.*
import io.mockk.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE, application = android.app.Application::class)
class SaleCommandsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "sale-command-test.db"
    private lateinit var db: AppDatabase
    private val merchant = "fixture-merchant"
    private val sale = Transaction(customerId = 1, amount = 50.0, isPaid = true, paymentMethodId = 1, date = 10)
    private val lines = listOf(InvoiceItem(id = "line", productId = "product", unitId = "unit", quantity = 2.0,
        pricePerUnit = 25.0, totalPrice = 50.0))
    private fun open(onQuery: ((String) -> Unit)? = null) {
        val builder=Room.databaseBuilder(context,AppDatabase::class.java,name).allowMainThreadQueries()
        if(onQuery!=null) builder.setQueryCallback({ sql, _ -> onQuery(sql) },java.util.concurrent.Executor { it.run() })
        db=builder.build()
    }
    @Before fun seed() = runBlocking {
        context.deleteDatabase(name); open()
        db.customerDao().upsertCustomer(CustomerEntity(id = 1, name = "fixture", createdAt = 0))
        db.productDao().insertProduct(ProductEntity("product", null, "fixture", "", null, merchant, 0, 0, "SYNCED"))
        db.productDao().insertUnit(ProductUnitEntity("unit", "product", "PIECE", "piece", 25.0,
            quantityInStock = 4.0, itemsPerCarton = null, lowStockThreshold = 0.0, isDefault = true,
            createdAt = 0, updatedAt = 0, syncStatus = "SYNCED"))
        db.cashBoxDao().upsert(CashBoxEntity("1", 1, "cash", currentBalance = 100.0, merchantId = merchant, updatedAt = 0))
    }
    @After fun close() { db.close(); context.deleteDatabase(name) }
    private fun snapshot(): Map<String, List<List<String?>>> = listOf("transactions", "invoice_items", "product_units",
        "stock_movements", "cash_boxes", "cash_box_movements", "return_invoices", "return_items", "sale_operations", "sale_outbox").associateWith { table ->
        db.openHelper.writableDatabase.query("SELECT * FROM $table ORDER BY rowid").use { c ->
            buildList { while (c.moveToNext()) add((0 until c.columnCount).map { if (c.isNull(it)) null else c.getString(it) }) }
        }
    }
    private fun failWrite(table: String, verb: String = "INSERT") {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER injected BEFORE $verb ON $table BEGIN SELECT RAISE(ABORT, 'injected failure'); END")
    }
    private suspend fun assertCreationRollback(table: String, verb: String = "INSERT") {
        val before = snapshot(); failWrite(table, verb)
        try { SaleCommands(db).create("create", merchant, sale, lines); fail("Expected injected write failure") }
        catch (expected: android.database.sqlite.SQLiteException) { }
        assertEquals("Every local row must roll back", before, snapshot())
        assertEquals(0.0, db.transactionDao().getUnpaidAmountByCustomer(1), 0.0)
    }
    @Test fun successfulCreation() = runBlocking {
        val id = SaleCommands(db).create("create", merchant, sale, lines)
        assertEquals(50.0, db.transactionDao().getTransactionById(id)!!.amount, 0.0)
        assertEquals(2.0, db.productDao().getQuantity("unit")!!, 0.0)
        assertEquals(150.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
        assertEquals(1, db.invoiceItemDao().getForTransactionOnce(id).size)
        assertEquals(-2.0, db.stockMovementDao().getAllOnce().single().quantity, 0.0)
        assertEquals(50.0, db.cashBoxMovementDao().getAllOnce().single().amountDelta, 0.0)
        assertEquals("PENDING", db.transactionDao().getTransactionById(id)!!.syncStatus)
    }
    @Test fun invoiceFailureRollsBackEverything() = runBlocking { assertCreationRollback("invoice_items") }
    @Test fun stockFailureRollsBackEverything() = runBlocking { assertCreationRollback("stock_movements") }
    @Test fun cashFailureRollsBackEverything() = runBlocking { assertCreationRollback("cash_box_movements") }
    @Test fun debtIsDerivedFromCommittedUnpaidSale() = runBlocking {
        SaleCommands(db).create("debt", merchant, sale.copy(isPaid = false), lines)
        assertEquals(50.0, db.transactionDao().getUnpaidAmountByCustomer(1), 0.0)
        assertEquals(100.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
        assertTrue(db.cashBoxMovementDao().getAllOnce().isEmpty())
    }
    @Test fun reopenCommittedSale() = runBlocking {
        val id = SaleCommands(db).create("create", merchant, sale, lines)
        val before = snapshot(); db.close(); open()
        assertEquals(before, snapshot())
        assertEquals(id, SaleCommands(db).create("create", merchant, sale, lines))
        assertEquals(before, snapshot())
    }
    @Test fun reopenRolledBackSale() = runBlocking {
        val before = snapshot(); failWrite("cash_box_movements")
        try { SaleCommands(db).create("create", merchant, sale, lines); fail("Expected failure") }
        catch (expected: android.database.sqlite.SQLiteException) { }
        db.close(); open(); assertEquals(before, snapshot())
    }
    private suspend fun original(): Transaction {
        val id = SaleCommands(db).create("create", merchant, sale, lines)
        return db.transactionDao().getTransactionById(id)!!.toDomain()
    }
    @Test fun successfulEditChangesInvoiceStockCashAndDebtTogether() = runBlocking {
        val old = original()
        SaleCommands(db).edit("edit", merchant, old, old.copy(amount = 75.0, isPaid = false),
            lines.map { it.copy(quantity = 3.0, totalPrice = 75.0) })
        assertEquals(1.0, db.productDao().getQuantity("unit")!!, 0.0)
        assertEquals(100.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
        assertEquals(75.0, db.transactionDao().getUnpaidAmountByCustomer(1), 0.0)
        assertEquals(3.0, db.invoiceItemDao().getForTransactionOnce(old.id).single().quantity, 0.0)
    }
    private suspend fun assertEditRollback(table: String, verb: String = "INSERT") {
        val old = original(); val before = snapshot(); failWrite(table, verb)
        try {
            SaleCommands(db).edit("edit", merchant, old, old.copy(amount = 75.0), lines.map { it.copy(quantity = 3.0, totalPrice = 75.0) })
            fail("Expected edit failure")
        } catch (expected: android.database.sqlite.SQLiteException) { }
        assertEquals(before, snapshot())
    }
    @Test fun editInvoiceFailureRollsBackRestoredStock() = runBlocking { assertEditRollback("invoice_items") }
    @Test fun editCashFailureRollsBackInvoiceStockAndSale() = runBlocking { assertEditRollback("cash_box_movements") }
    @Test fun editStockFailureRollsBackEverything() = runBlocking { assertEditRollback("stock_movements") }

    @Test fun successfulDeletionRestoresAllNetEffects() = runBlocking {
        val old = original()
        SaleCommands(db).delete("delete", merchant, old)
        assertNull(db.transactionDao().getTransactionById(old.id))
        assertTrue(db.invoiceItemDao().getForTransactionOnce(old.id).isEmpty())
        assertEquals(4.0, db.productDao().getQuantity("unit")!!, 0.0)
        assertEquals(100.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
        assertEquals(0.0, db.transactionDao().getUnpaidAmountByCustomer(1), 0.0)
        assertEquals(0.0, db.stockMovementDao().getAllOnce().sumOf { it.quantity }, 0.0)
        assertEquals(0.0, db.cashBoxMovementDao().getAllOnce().sumOf { it.amountDelta }, 0.0)
    }
    private suspend fun assertDeleteRollback(table: String, verb: String) {
        val old = original(); val before = snapshot(); failWrite(table, verb)
        try { SaleCommands(db).delete("delete", merchant, old); fail("Expected delete failure") }
        catch (expected: android.database.sqlite.SQLiteException) { }
        assertEquals(before, snapshot())
    }
    @Test fun deleteCashFailureRestoresEveryRow() = runBlocking { assertDeleteRollback("cash_box_movements", "INSERT") }
    @Test fun deleteInvoiceFailureRestoresStockAndReturnLedger() = runBlocking { assertDeleteRollback("invoice_items", "DELETE") }
    @Test fun deleteSaleFailureRestoresEveryRow() = runBlocking { assertDeleteRollback("transactions", "DELETE") }

    private suspend fun returned(quantity: Double): Transaction {
        val old = original()
        val refund = quantity * 25.0
        db.returnDao().insertReturnWithItems(
            ReturnInvoiceEntity("return", old.id, merchant, "PARTIAL", refund, "", 1, "PENDING"),
            listOf(ReturnItemEntity("returned", "return", "product", "fixture", "unit", "piece", 2.0, quantity, 25.0, 0.0, refund, 0.0)))
        db.productDao().updateQuantity("unit", 2.0 + quantity)
        db.cashBoxDao().applyDelta("1", -refund, 1)
        db.stockMovementDao().insert(StockMovementEntity("return-stock", "product", "fixture", "unit", "piece", "RETURN_IN",
            quantity, 2.0, 2.0 + quantity, old.id, "", merchant, 1, "PENDING"))
        db.cashBoxMovementDao().insert(CashBoxMovementEntity("return-cash", "1", "cash", "TRANSACTION_EFFECT", -refund,
            150.0 - refund, relatedTransactionId = old.id, createdAt = 1, merchantId = merchant))
        val result = old.copy(amount = 50.0 - refund, originalAmount = 50.0,
            returnStatus = if (quantity == 2.0) TransactionReturnStatus.FULLY_RETURNED else TransactionReturnStatus.PARTIALLY_RETURNED)
        db.transactionDao().updateTransaction(TransactionEntity.fromDomain(result))
        return result
    }
    private suspend fun deleteReturned(quantity: Double) {
        val old = returned(quantity)
        SaleCommands(db).delete("delete", merchant, old)
        assertNull(db.transactionDao().getTransactionById(old.id))
        assertTrue(db.invoiceItemDao().getForTransactionOnce(old.id).isEmpty())
        assertTrue(db.returnDao().getAllInvoicesOnce().isEmpty())
        assertTrue(db.returnDao().getAllItemsOnce().isEmpty())
        assertEquals(0.0, db.stockMovementDao().getAllOnce().sumOf { it.quantity }, 0.0)
        assertEquals(0.0, db.cashBoxMovementDao().getAllOnce().sumOf { it.amountDelta }, 0.0)
        assertEquals(4.0, db.productDao().getQuantity("unit")!!, 0.0)
        assertEquals(100.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
        assertEquals(0.0, db.transactionDao().getUnpaidAmountByCustomer(1), 0.0)
    }
    @Test fun partialReturnDeletionRestoresOnlyRemainingStockAndCash() = runBlocking { deleteReturned(1.0) }
    @Test fun fullReturnDeletionDoesNotRestoreOrRefundTwice() = runBlocking { deleteReturned(2.0) }
    @Test fun deleteFailurePreservesPartialReturnLedgerAndRefundState() = runBlocking {
        val old = returned(1.0); val before = snapshot(); failWrite("cash_box_movements")
        try { SaleCommands(db).delete("delete", merchant, old); fail("Expected failure") }
        catch (expected: android.database.sqlite.SQLiteException) { }
        assertEquals(before, snapshot())
    }
    @Test fun duplicateSubmissionHasExactlyOneLocalEffect() = runBlocking {
        val commands = SaleCommands(db)
        val id = commands.create("same", merchant, sale, lines); val before = snapshot()
        assertEquals(id, commands.create("same", merchant, sale, lines))
        assertEquals(before, snapshot())
    }
    @Test fun operationIdCannotBeReusedWithDifferentPayload() = runBlocking {
        val commands = SaleCommands(db)
        commands.create("same", merchant, sale, lines); val before = snapshot()
        try { commands.create("same", merchant, sale.copy(amount = 100.0), lines); fail("Expected mismatch") }
        catch (expected: IllegalStateException) { }
        assertEquals(before, snapshot())
    }
    @Test fun repeatedDeletionDoesNotApplyStockOrCashAgain() = runBlocking {
        val old = original(); val commands = SaleCommands(db)
        commands.delete("delete", merchant, old); val before = snapshot()
        commands.delete("delete", merchant, old)
        assertEquals(before, snapshot())
        commands.delete("second-delete", merchant, old)
        assertEquals(4.0, db.productDao().getQuantity("unit")!!, 0.0)
        assertEquals(100.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
        assertTrue(db.transactionDao().getAllOnce().isEmpty())
    }
    @Test fun concurrentLastStockAllowsOnlyOneSale() = runBlocking {
        db.productDao().updateQuantity("unit", 2.0)
        val results = (1..2).map { n -> async(Dispatchers.Default) {
            runCatching { SaleCommands(db).create("sale-$n", merchant, sale, lines.map { it.copy(id = "line-$n") }) }
        } }.awaitAll()
        assertEquals(1, results.count { it.isSuccess }); assertEquals(1, results.count { it.isFailure })
        assertEquals(0.0, db.productDao().getQuantity("unit")!!, 0.0)
        assertEquals(1, db.transactionDao().getAllOnce().size)
        assertEquals(1, db.invoiceItemDao().getAllOnce().size)
        assertEquals(1, db.stockMovementDao().getAllOnce().size)
        assertEquals(1, db.cashBoxMovementDao().getAllOnce().size)
        assertEquals(150.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
    }
    @Test fun cancellationAfterWritesRollsBackAllTables() = runBlocking {
        val before = snapshot(); val reached = CompletableDeferred<Unit>()
        val commands = SaleCommands(db) { reached.complete(Unit); awaitCancellation() }
        val job = launch(Dispatchers.Default) { commands.create("cancel", merchant, sale, lines) }
        withTimeout(10000) { reached.await() }; job.cancelAndJoin()
        assertEquals(before, snapshot())
        db.close(); open(); assertEquals(before, snapshot())
        SaleCommands(db).create("cancel", merchant, sale, lines)
        assertEquals(1, db.transactionDao().getAllOnce().size)
    }
    @Test fun staleEditIsRejectedAndReplayCannotOverwriteLaterEdit() = runBlocking {
        val old = original(); val commands = SaleCommands(db)
        val edited = old.copy(amount = 60.0)
        commands.edit("edit-one", merchant, old, edited, null)
        val before = snapshot()
        try { commands.edit("stale", merchant, old, old.copy(amount = 90.0), null); fail("Stale edit accepted") }
        catch (expected: IllegalStateException) { }
        assertEquals(before, snapshot())
        val latest = db.transactionDao().getTransactionById(old.id)!!.toDomain()
        commands.edit("edit-two", merchant, latest, latest.copy(amount = 70.0), null)
        val after = snapshot()
        commands.edit("edit-one", merchant, old, edited, null)
        assertEquals(after, snapshot())
    }
    @Test fun returnedSaleEditPreservesRefundAndUsesRemainingQuantities() = runBlocking {
        val old = returned(1.0)
        SaleCommands(db).edit("edit", merchant, old, old.copy(amount = 75.0), lines.map { it.copy(quantity = 3.0, totalPrice = 75.0) })
        val result = db.transactionDao().getTransactionById(old.id)!!.toDomain()
        assertEquals(50.0, result.amount, 0.0); assertEquals(75.0, result.originalAmount, 0.0)
        assertEquals(2.0, db.productDao().getQuantity("unit")!!, 0.0)
        assertEquals(150.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
        assertEquals(1.0, db.returnDao().totalReturnedForUnit(old.id, "unit"), 0.0)
    }

    @Test fun addingStockToFullyReturnedInvoiceMakesItPartiallyReturned() = runBlocking {
        val old = returned(2.0)
        SaleCommands(db).edit("edit", merchant, old, old.copy(amount = 75.0), lines.map { it.copy(quantity = 3.0, totalPrice = 75.0) })
        val actual = db.transactionDao().getTransactionById(old.id)!!.toDomain()
        assertEquals(TransactionReturnStatus.PARTIALLY_RETURNED, actual.returnStatus)
        assertEquals(25.0, actual.amount, 0.0)
        assertEquals(3.0, db.productDao().getQuantity("unit")!!, 0.0)
        assertEquals(125.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
    }
    @Test fun unpaidDeletionClearsDerivedDebtWithoutCashEffect() = runBlocking {
        val id = SaleCommands(db).create("debt", merchant, sale.copy(isPaid = false), lines)
        val old = db.transactionDao().getTransactionById(id)!!.toDomain()
        SaleCommands(db).delete("delete", merchant, old)
        assertEquals(0.0, db.transactionDao().getUnpaidAmountByCustomer(1), 0.0)
        assertEquals(4.0, db.productDao().getQuantity("unit")!!, 0.0)
        assertEquals(100.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
        assertTrue(db.cashBoxMovementDao().getAllOnce().isEmpty())
    }
    @Test fun concurrentIdenticalSubmissionsApplyOnce() = runBlocking {
        val ids = (1..2).map { async(Dispatchers.Default) { SaleCommands(db).create("same", merchant, sale, lines) } }.awaitAll()
        assertEquals(ids[0], ids[1])
        assertEquals(1, db.transactionDao().getAllOnce().size)
        assertEquals(1, db.stockMovementDao().getAllOnce().size)
        assertEquals(1, db.cashBoxMovementDao().getAllOnce().size)
        assertEquals(2.0, db.productDao().getQuantity("unit")!!, 0.0)
        assertEquals(150.0, db.cashBoxDao().getById("1")!!.currentBalance, 0.0)
    }
    private suspend fun cancelMutation(delete: Boolean) {
        val old = original(); val before = snapshot(); val reached = CompletableDeferred<Unit>()
        val commands = SaleCommands(db) { reached.complete(Unit); awaitCancellation() }
        val job = launchInDefault {
            if (delete) commands.delete("delete", merchant, old)
            else commands.edit("edit", merchant, old, old.copy(amount = 75.0), lines.map { it.copy(quantity = 3.0, totalPrice = 75.0) })
        }
        withTimeout(10000) { reached.await() }; job.cancelAndJoin()
        assertEquals(before, snapshot())
    }
    private fun launchInDefault(block: suspend () -> Unit): Job = CoroutineScope(Dispatchers.Default).launch { block() }
    @Test fun cancelledEditRestoresAllRows() = runBlocking { cancelMutation(false) }
    @Test fun cancelledDeleteRestoresAllRows() = runBlocking { cancelMutation(true) }
    @Test fun backupRoundTripPreservesReceiptAndDoesNotReplaySale() = runBlocking {
        val id = SaleCommands(db).create("create", merchant, sale, lines)
        val manager = com.trader.salesmanager.ui.settings.backup.BackupManager(context, db, merchant)
        val file = manager.exportEncryptedBackup("fixture-password".toCharArray())
        try {
            val payload = manager.parseBackupFromBytes(file.readBytes(), "fixture-password".toCharArray())
            manager.restorePayload(payload)
            val before = snapshot()
            assertEquals(id, SaleCommands(db).create("create", merchant, sale, lines))
            assertEquals(before, snapshot())
        } finally { file.delete() }
    }
    @Test fun failedBackupRestorePreservesReceiptAndSale() = runBlocking {
        original(); val before = snapshot()
        val manager = com.trader.salesmanager.ui.settings.backup.BackupManager(context, db, merchant)
        val corrupt = com.trader.salesmanager.ui.settings.backup.BackupPayload(
            customers = listOf(CustomerEntity(id = 1, name = "fixture", createdAt = 0)),
            transactions = listOf(TransactionEntity(customerId = 999, amount = 1.0, isPaid = false)),
            saleOperations = db.saleOperationDao().allForMerchant(merchant), saleOutbox = db.saleOutboxDao().all())
        try { manager.restorePayload(corrupt); fail("Expected orphan restore failure") }
        catch (expected: android.database.sqlite.SQLiteException) { }
        assertEquals(before, snapshot())
    }
    @Test fun invoiceIdentityCannotOverwriteAnotherSale() = runBlocking {
        original(); val before = snapshot()
        try { SaleCommands(db).create("another", merchant, sale, lines); fail("Expected line collision") }
        catch (expected: IllegalArgumentException) { }
        assertEquals(before, snapshot())
    }
    @Test fun wrongMerchantCannotEditOrDeleteExistingSale() = runBlocking {
        val old = original(); val before = snapshot()
        try { SaleCommands(db).edit("edit", "other", old, old.copy(amount = 60.0), null); fail("Wrong merchant accepted") }
        catch (expected: IllegalArgumentException) { }
        try { SaleCommands(db).delete("delete", "other", old); fail("Wrong merchant accepted") }
        catch (expected: IllegalArgumentException) { }
        assertEquals(before, snapshot())
    }

    @Test fun commitCreatesExactlyOneOutboxEventAndReplayDoesNotDuplicateIt() = runBlocking {
        val commands=SaleCommands(db)
        val id=commands.create("outbox-create",merchant,sale,lines)
        commands.create("outbox-create",merchant,sale,lines)
        val event=db.saleOutboxDao().all().single()
        assertEquals(id,event.aggregateId);assertEquals(1L,event.aggregateRevision)
        assertEquals("CREATE",event.eventType);assertEquals("PENDING",event.state)
        assertEquals("outbox-create",event.operationId)
        assertTrue(event.payload.contains("transactions/$id"))
    }
    @Test fun editAndDeletionCreateVersionedOutboxTombstone() = runBlocking {
        val old=original();val commands=SaleCommands(db)
        commands.edit("edit",merchant,old,old.copy(amount=60.0),null)
        val edited=db.transactionDao().getTransactionById(old.id)!!.toDomain()
        commands.delete("delete",merchant,edited)
        val events=db.saleOutboxDao().all()
        assertEquals(listOf(1L,2L,3L),events.map { it.aggregateRevision })
        assertEquals(listOf("CREATE","EDIT","DELETE"),events.map { it.eventType })
        assertTrue(events.last().payload.contains("transactions/${old.id}"))
        assertNull(db.transactionDao().getTransactionById(old.id))
    }
    @Test fun outboxInsertFailureRollsBackSaleStockCashAndReceipt() = runBlocking { assertCreationRollback("sale_outbox") }
    @Test fun restartKeepsCommittedOutboxPayloadUnchanged() = runBlocking {
        original();val before=db.saleOutboxDao().all();assertEquals(1,before.size)
        db.close();open();assertEquals(before,db.saleOutboxDao().all())
    }

    private fun outboxDispatcher(remote: com.trader.salesmanager.sales.outbox.OutboxTransport, clock: () -> Long = System::currentTimeMillis): com.trader.salesmanager.sales.outbox.OutboxDispatcher {
        val session=kotlinx.coroutines.flow.MutableStateFlow(merchant)
        return com.trader.salesmanager.sales.outbox.OutboxDispatcher(db,remote,session,{merchant},clock=clock)
    }
    @Test fun createThenEditDeliversBothImmutableRevisionsInOrder() = runBlocking {
        val old=original()
        SaleCommands(db).edit("edit",merchant,old,old.copy(amount=75.0),lines.map { it.copy(quantity=3.0,totalPrice=75.0) })
        val remote=com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport(db.saleOutboxDao().all().first())
        val dispatcher=outboxDispatcher(remote)
        repeat(2) { assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,dispatcher.once(merchant)) }
        assertEquals(75.0,(remote.values["RTDB" to "transactions/${old.id}"]!!["amount"] as Number).toDouble(),0.0)
        assertEquals(1.0,(remote.values["FIRESTORE" to "products/product/units/unit"]!!["quantityInStock"] as Number).toDouble(),0.0)
        assertTrue(db.saleOutboxDao().all().all { it.state=="ACKNOWLEDGED" })
        assertEquals("SYNCED",db.transactionDao().getTransactionById(old.id)!!.syncStatus)
    }
    @Test fun createThenDeleteDeliversTombstoneAndNetZeroCashAndStock() = runBlocking {
        val old=original();SaleCommands(db).delete("delete",merchant,old)
        val remote=com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport(db.saleOutboxDao().all().first())
        val dispatcher=outboxDispatcher(remote)
        repeat(2) { assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,dispatcher.once(merchant)) }
        assertNull(remote.values["RTDB" to "transactions/${old.id}"])
        assertNull(remote.values["RTDB" to "invoice_items/line"])
        assertEquals(4.0,(remote.values["FIRESTORE" to "products/product/units/unit"]!!["quantityInStock"] as Number).toDouble(),0.0)
        assertEquals(100.0,(remote.values["RTDB" to "cash_boxes/1"]!!["currentBalance"] as Number).toDouble(),0.0)
        assertEquals("DELETE",db.saleOutboxDao().all().last().eventType)
    }
    @Test fun relatedReturnAndPartialDeletionAreDurableAndDeliverableInOneSequence() = runBlocking {
        val old=original();val commands=SaleCommands(db)
        val request=ReturnInvoice(id="return",originalTransactionId=old.id,merchantId=merchant,totalRefund=25.0)
        commands.journalReturn(request,emptyList()) {
            returned(1.0)
            db.returnDao().getById("return")!!.toDomain()
        }
        val updated=db.transactionDao().getTransactionById(old.id)!!.toDomain()
        commands.delete("delete",merchant,updated)
        assertEquals(listOf("CREATE","RETURN","DELETE"),db.saleOutboxDao().all().map { it.eventType })
        val remote=com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport(db.saleOutboxDao().all().first())
        val dispatcher=outboxDispatcher(remote)
        repeat(3) { assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,dispatcher.once(merchant)) }
        assertNull(remote.values["RTDB" to "transactions/${old.id}"])
        assertNull(remote.values["RTDB" to "return_invoices/return"])
        assertEquals(4.0,(remote.values["FIRESTORE" to "products/product/units/unit"]!!["quantityInStock"] as Number).toDouble(),0.0)
        assertEquals(100.0,(remote.values["RTDB" to "cash_boxes/1"]!!["currentBalance"] as Number).toDouble(),0.0)
    }
    @Test fun restoreDuringDispatchInvalidatesOldLeaseAndReplaysRemoteReceiptSafely() = runBlocking {
        original();val event=db.saleOutboxDao().all().single()
        val remote=com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport(event)
        val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        remote.pauseAfterCommit={ entered.complete(Unit);release.await() }
        val first=async(Dispatchers.Default) { outboxDispatcher(remote).once(merchant) }
        withTimeout(10000) { entered.await() }
        val manager=com.trader.salesmanager.ui.settings.backup.BackupManager(context,db,merchant)
        val file=manager.exportEncryptedBackup("fixture-password".toCharArray())
        try {
            val payload=manager.parseBackupFromBytes(file.readBytes(),"fixture-password".toCharArray())
            assertEquals("PROCESSING",payload.saleOutbox.single().state)
            manager.restorePayload(payload)
            assertEquals("PENDING",db.saleOutboxDao().all().single().state)
            assertNull(db.saleOutboxDao().all().single().leaseOwner)
            remote.pauseAfterCommit=null
            val writes=remote.writes
            assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,outboxDispatcher(remote).once(merchant))
            assertEquals(writes,remote.writes)
            release.complete(Unit)
            assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.STALE,first.await())
        } finally { release.complete(Unit);file.delete() }
    }
    @Test fun committedDeletionConflictRemainsVisibleAndNeverDisappears() = runBlocking {
        val old=original();SaleCommands(db).delete("delete",merchant,old)
        val remote=com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport(db.saleOutboxDao().all().first())
        remote.values["FIRESTORE" to "products/product/units/unit"]=mapOf("quantityInStock" to 999.0)
        assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.CONFLICT,outboxDispatcher(remote).once(merchant))
        assertEquals("CONFLICTING",db.saleOutboxDao().all().first().state)
        assertEquals("PENDING",db.saleOutboxDao().all().last().state)
        assertNull(db.transactionDao().getTransactionById(old.id))
        val observer=com.trader.core.sync.SyncStatusObserver(db.transactionDao(),db.cashBoxDao(),db.cashBoxMovementDao(),
            db.stockMovementDao(),db.invoiceItemDao(),db.saleOutboxDao())
        val state=withTimeout(10000) { observer.globalSyncState.first { it is com.trader.core.sync.GlobalSyncState.Failed } }
        assertTrue(state is com.trader.core.sync.GlobalSyncState.Failed)
    }
    @Test fun staleUiMerchantCannotCreateAnEventUnderThePreviousSession() = runBlocking {
        val before=snapshot()
        try { SaleCommands(db,authorizedMerchant={"other"}).create("create",merchant,sale,lines);fail("Stale merchant accepted") }
        catch(expected:IllegalArgumentException) {}
        assertEquals(before,snapshot())
    }

    private suspend fun realReturnPipeline(injectFailure:Boolean) {
        val old=original();val before=snapshot();val commands=SaleCommands(db)
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Unconfined)
        val activation=mockk<com.trader.core.domain.repository.ActivationRepository>()
        coEvery { activation.getMerchantCode() } returns merchant
        every { activation.observeMerchantCode() } returns kotlinx.coroutines.flow.emptyFlow()
        val sync=mockk<com.trader.core.data.remote.FirebaseSyncService>(relaxed=true)
        every { sync.observeInvoiceItems(any()) } returns kotlinx.coroutines.flow.emptyFlow()
        val productRemote=mockk<com.trader.core.data.remote.ProductFirestoreService>(relaxed=true)
        val invoiceRepo=com.trader.core.data.repository.InvoiceItemRepositoryImpl(db.invoiceItemDao(),productRemote,sync,merchant,db,activation)
        val stockRepo=com.trader.core.data.repository.StockRepositoryImpl(db.productDao(),db.stockMovementDao(),productRemote,merchant,db,activation)
        val cashRepo=com.trader.core.data.repository.CashBoxRepositoryImpl(db,db.cashBoxDao(),db.cashBoxMovementDao(),db.paymentMethodDao(),sync,activation,scope)
        val txRepo=com.trader.core.data.repository.TransactionRepositoryImpl(db.transactionDao(),db.customerDao(),db.paymentMethodDao(),sync,
            activation,invoiceRepo,stockRepo,cashRepo,db.returnDao(),scope,db)
        val returns=com.trader.core.data.repository.ReturnRepositoryImpl(db,stockRepo,txRepo,invoiceRepo,merchant,activation,sync,scope,commands::journalReturn)
        val request=ReturnInvoice(id="real-return",originalTransactionId=old.id,totalRefund=25.0)
        val items=listOf(ReturnItem(id="returned-item",returnInvoiceId=request.id,productId="product",unitId="unit",
            originalQuantity=2.0,returnedQuantity=1.0,pricePerUnit=25.0))
        try {
            if(injectFailure) failWrite("sale_outbox")
            try { returns.processReturn(request,items);if(injectFailure) fail("Expected outbox failure") }
            catch(expected:android.database.sqlite.SQLiteException) { if(!injectFailure) throw expected }
            if(injectFailure) assertEquals(before,snapshot())
            else {
                assertEquals(listOf("CREATE","RETURN"),db.saleOutboxDao().all().map { it.eventType })
                assertEquals(3.0,db.productDao().getQuantity("unit")!!,0.0)
                assertEquals(125.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
                assertEquals(25.0,db.transactionDao().getTransactionById(old.id)!!.amount,0.0)
                returns.processReturn(request,items)
                assertEquals(2,db.saleOutboxDao().all().size)
                txRepo.syncPendingTransactions();stockRepo.syncPendingMovements();cashRepo.syncPendingBoxes();invoiceRepo.syncPendingItems();returns.syncPendingReturns()
                commands.delete("delete",merchant,db.transactionDao().getTransactionById(old.id)!!.toDomain())
                val remote=com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport(db.saleOutboxDao().all().first())
                repeat(3) { assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,outboxDispatcher(remote).once(merchant)) }
                assertEquals(100.0,(remote.values["RTDB" to "cash_boxes/1"]!!["currentBalance"] as Number).toDouble(),0.0)
            }
            coVerify(exactly=0) { sync.pushTransaction(any(),any()) }
            coVerify(exactly=0) { sync.pushCashBox(any(),any()) }
            coVerify(exactly=0) { sync.pushCashBoxMovement(any(),any()) }
            coVerify(exactly=0) { sync.pushReturnInvoice(any(),any(),any()) }
            coVerify(exactly=0) { productRemote.uploadMovement(any(),any()) }
            coVerify(exactly=0) { productRemote.updateRemoteQuantity(any(),any(),any(),any()) }
        } finally { scope.coroutineContext.job.cancelAndJoin() }
    }
    @Test fun actualReturnRepositoryJournalsStockCashAndReturnBeforeLegacyUploads() = runBlocking { realReturnPipeline(false) }
    @Test fun actualReturnRepositoryRollsBackAllEffectsIfOutboxCannotPersist() = runBlocking { realReturnPipeline(true) }
    @Test fun oldAcknowledgedRestoreBecomesConflictIfRemoteRevisionHasAdvanced() = runBlocking {
        val old=original();val first=db.saleOutboxDao().all().single()
        val remote=com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport(first)
        val manager=com.trader.salesmanager.ui.settings.backup.BackupManager(context,db,merchant)
        val file=manager.exportEncryptedBackup("fixture-password".toCharArray())
        try {
            val backup=manager.parseBackupFromBytes(file.readBytes(),"fixture-password".toCharArray())
            assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,outboxDispatcher(remote).once(merchant))
            remote.advanceAggregate(old.id,2)
            remote.values["RTDB" to "transactions/${old.id}"] = remote.values["RTDB" to "transactions/${old.id}"].orEmpty() + ("amount" to 60.0)
            manager.restorePayload(backup)
            assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.CONFLICT,outboxDispatcher(remote).once(merchant))
            assertEquals("CONFLICTING",db.saleOutboxDao().all().single().state)
            assertEquals(60.0,(remote.values["RTDB" to "transactions/${old.id}"]!!["amount"] as Number).toDouble(),0.0)
        } finally { file.delete() }
    }

    private suspend fun inspectLegacyTransactionStream(managed:Boolean) {
        val old=if(managed) original() else sale.copy(id=8,syncStatus=SyncStatus.SYNCED).also {
            db.transactionDao().insertTransaction(TransactionEntity.fromDomain(it))
            db.invoiceItemDao().insertAll(lines.map { line->line.copy(transactionId=8,merchantId=merchant).toEntity() })
        }
        val commands=kotlinx.coroutines.channels.Channel<Pair<List<Transaction>,CompletableDeferred<Unit>>>()
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Unconfined)
        val activation=mockk<com.trader.core.domain.repository.ActivationRepository>()
        every { activation.observeMerchantCode() } returns kotlinx.coroutines.flow.flowOf(merchant)
        coEvery { activation.getMerchantCode() } returns merchant
        val sync=mockk<com.trader.core.data.remote.FirebaseSyncService>(relaxed=true)
        every { sync.observeTransactions(merchant) } returns kotlinx.coroutines.flow.flow {
            for((records,finished) in commands) { emit(records);finished.complete(Unit) }
        }
        val invoice=mockk<com.trader.core.domain.repository.InvoiceItemRepository>(relaxed=true)
        val stock=mockk<com.trader.core.domain.repository.StockRepository>(relaxed=true)
        val cash=mockk<com.trader.core.domain.repository.CashBoxRepository>(relaxed=true)
        com.trader.core.data.repository.TransactionRepositoryImpl(db.transactionDao(),db.customerDao(),db.paymentMethodDao(),sync,
            activation,invoice,stock,cash,db.returnDao(),scope,db)
        suspend fun emit(records:List<Transaction>) {
            val finished=CompletableDeferred<Unit>();commands.send(records to finished);withTimeout(10000) { finished.await() }
        }
        try {
            if(managed) {
                emit(listOf(old.copy(amount=999.0)))
                assertEquals(50.0,db.transactionDao().getTransactionById(old.id)!!.amount,0.0)
                SaleCommands(db).delete("delete",merchant,old)
                emit(listOf(old))
                assertNull(db.transactionDao().getTransactionById(old.id))
                assertTrue(db.invoiceItemDao().getForTransactionOnce(old.id).isEmpty())
                assertEquals(4.0,db.productDao().getQuantity("unit")!!,0.0)
                assertEquals(100.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
            } else {
                emit(emptyList())
                assertEquals("CONFLICT",db.transactionDao().getTransactionById(old.id)!!.syncStatus)
                assertEquals(50.0,db.transactionDao().getTransactionById(old.id)!!.amount,0.0)
                assertEquals(1,db.invoiceItemDao().getForTransactionOnce(old.id).size)
                coVerify(exactly=0) { stock.returnStock(any(),any(),any(),any(),any(),any()) }
                coVerify(exactly=0) { cash.applyTransactionEffect(any(),any(),any(),any(),any(),any(),any()) }
            }
        } finally { commands.close();scope.coroutineContext.job.cancelAndJoin() }
    }
    @Test fun unversionedTransactionEchoCannotOverwriteOrResurrectManagedSale()=runBlocking { inspectLegacyTransactionStream(true) }
    @Test fun unversionedRemoteDeletionIsQuarantinedWithoutGuessingFinancialEffects()=runBlocking { inspectLegacyTransactionStream(false) }
    @Test fun unversionedInvoiceEchoCannotResurrectDeletedManagedLines()=runBlocking {
        val old=original()
        val messages=kotlinx.coroutines.channels.Channel<Pair<List<InvoiceItem>,CompletableDeferred<Unit>>>()
        val activation=mockk<com.trader.core.domain.repository.ActivationRepository>()
        every { activation.observeMerchantCode() } returns kotlinx.coroutines.flow.flowOf(merchant)
        coEvery { activation.getMerchantCode() } returns merchant
        val sync=mockk<com.trader.core.data.remote.FirebaseSyncService>(relaxed=true)
        every { sync.observeInvoiceItems(merchant) } returns kotlinx.coroutines.flow.flow {
            for((records,finished) in messages) { emit(records);finished.complete(Unit) }
        }
        val remote=mockk<com.trader.core.data.remote.ProductFirestoreService>(relaxed=true)
        com.trader.core.data.repository.InvoiceItemRepositoryImpl(db.invoiceItemDao(),remote,sync,merchant,db,activation)
        suspend fun emit() {
            val done=CompletableDeferred<Unit>()
            messages.send(lines.map { it.copy(transactionId=old.id,merchantId=merchant,quantity=999.0) } to done)
            withTimeout(10000) { done.await() }
        }
        try {
            emit();assertEquals(2.0,db.invoiceItemDao().getForTransactionOnce(old.id).single().quantity,0.0)
            SaleCommands(db).delete("delete",merchant,old)
            emit();assertTrue(db.invoiceItemDao().getForTransactionOnce(old.id).isEmpty())
        } finally { messages.close() }
    }

    @Test fun olderBackupCannotEraseAnUncertainDeletionTombstone()=runBlocking {
        val old=original();val manager=com.trader.salesmanager.ui.settings.backup.BackupManager(context,db,merchant)
        val file=manager.exportEncryptedBackup("fixture-password".toCharArray())
        try {
            val oldBackup=manager.parseBackupFromBytes(file.readBytes(),"fixture-password".toCharArray())
            SaleCommands(db).delete("delete",merchant,old)
            val before=snapshot()
            try { manager.restorePayload(oldBackup);fail("Restore discarded a committed deletion intent") }
            catch(expected:IllegalArgumentException) {}
            assertEquals(before,snapshot())
            assertEquals("DELETE",db.saleOutboxDao().all().last().eventType)
        } finally { file.delete() }
    }

    @Test fun acknowledgedBackupRevalidatesLatestRevisionWithoutReplayingHistoricalEffects()=runBlocking {
        val old=original();SaleCommands(db).edit("edit",merchant,old,old.copy(amount=60.0),null)
        val remote=com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport(db.saleOutboxDao().all().first())
        val dispatcher=outboxDispatcher(remote)
        repeat(2) { assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,dispatcher.once(merchant)) }
        val manager=com.trader.salesmanager.ui.settings.backup.BackupManager(context,db,merchant)
        val file=manager.exportEncryptedBackup("fixture-password".toCharArray())
        try {
            val payload=manager.parseBackupFromBytes(file.readBytes(),"fixture-password".toCharArray())
            manager.restorePayload(payload)
            assertEquals(listOf("ACKNOWLEDGED","PENDING"),db.saleOutboxDao().all().map { it.state })
            val writes=remote.writes
            assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,dispatcher.once(merchant))
            assertEquals(writes,remote.writes)
        } finally { file.delete() }
    }
    @Test fun restoreCannotRewriteExistingImmutablePayloadUnderTheSameEventId()=runBlocking {
        original();val manager=com.trader.salesmanager.ui.settings.backup.BackupManager(context,db,merchant)
        val file=manager.exportEncryptedBackup("fixture-password".toCharArray())
        try {
            val payload=manager.parseBackupFromBytes(file.readBytes(),"fixture-password".toCharArray())
            val before=snapshot()
            try { manager.restorePayload(payload.copy(saleOutbox=payload.saleOutbox.map { it.copy(payload="{}") }));fail("Immutable event changed") }
            catch(expected:IllegalArgumentException) {}
            assertEquals(before,snapshot())
        } finally { file.delete() }
    }

    @Test fun freshRestoreDoesNotReuseADeletedAggregateIdentity()=runBlocking {
        val old=original();SaleCommands(db).delete("delete",merchant,old)
        val manager=com.trader.salesmanager.ui.settings.backup.BackupManager(context,db,merchant)
        val file=manager.exportEncryptedBackup("fixture-password".toCharArray())
        try {
            val payload=manager.parseBackupFromBytes(file.readBytes(),"fixture-password".toCharArray())
            db.close();context.deleteDatabase(name);open()
            com.trader.salesmanager.ui.settings.backup.BackupManager(context,db,merchant).restorePayload(payload)
            val next=SaleCommands(db).create("new-create",merchant,sale,lines.map { it.copy(id="new-line") })
            assertTrue("Deleted aggregate identity reused",next>old.id)
            assertNull(db.transactionDao().getTransactionById(old.id))
            assertEquals(listOf(1L,2L,1L),db.saleOutboxDao().all().map { it.aggregateRevision })
        } finally { file.delete() }
    }

    @Test fun realEditDuringOldUploadKeepsNewFinancialRowsPendingUntilItsOwnReceipt()=runBlocking {
        val old=original()
        val remote=com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport(db.saleOutboxDao().all().single())
        val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        remote.pauseAfterCommit={ entered.complete(Unit);release.await() }
        val first=async(Dispatchers.Default) { outboxDispatcher(remote).once(merchant) }
        try {
            withTimeout(10000) { entered.await() }
            withTimeout(10000) { SaleCommands(db).edit("edit",merchant,old,old.copy(amount=75.0),lines.map { it.copy(quantity=3.0,totalPrice=75.0) }) }
            remote.pauseAfterCommit=null;release.complete(Unit)
            assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,first.await())
            val newer=db.transactionDao().getTransactionById(old.id)!!
            assertEquals(75.0,newer.amount,0.0);assertEquals("PENDING",newer.syncStatus)
            assertEquals(1.0,db.productDao().getQuantity("unit")!!,0.0)
            assertEquals(175.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
            assertEquals("PENDING",db.cashBoxDao().getById("1")!!.syncStatus)
            assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,outboxDispatcher(remote).once(merchant))
            assertEquals("SYNCED",db.transactionDao().getTransactionById(old.id)!!.syncStatus)
            assertEquals(75.0,(remote.values["RTDB" to "transactions/${old.id}"]!!["amount"] as Number).toDouble(),0.0)
        } finally { withContext(NonCancellable) { release.complete(Unit);first.cancelAndJoin() } }
    }

    @Test fun identicalContentRevisionIsNotAcknowledgedByAnOlderEventReceipt()=runBlocking {
        val old=original()
        val remote=com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport(db.saleOutboxDao().all().single())
        val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        remote.pauseAfterCommit={ entered.complete(Unit);release.await() }
        val first=async(Dispatchers.Default) { outboxDispatcher(remote).once(merchant) }
        try {
            withTimeout(10000) { entered.await() }
            SaleCommands(db).edit("identical-edit",merchant,old,old,null)
            remote.pauseAfterCommit=null;release.complete(Unit)
            assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,first.await())
            assertEquals("PENDING",db.transactionDao().getTransactionById(old.id)!!.syncStatus)
            assertEquals(listOf("ACKNOWLEDGED","PENDING"),db.saleOutboxDao().all().map { it.state })
            assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,outboxDispatcher(remote).once(merchant))
            assertEquals("SYNCED",db.transactionDao().getTransactionById(old.id)!!.syncStatus)
            assertEquals(150.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
        } finally { withContext(NonCancellable) { release.complete(Unit);first.cancelAndJoin() } }
    }

    private suspend fun acknowledgeTwoSales(restart: Boolean) {
        val first=original()
        val seed=db.saleOutboxDao().all().single()
        val second=SaleCommands(db).create("second-sale",merchant,sale,lines.map { it.copy(id="second-line") })
        val remote=com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport(seed)
        assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,outboxDispatcher(remote).once(merchant))
        assertEquals("PENDING",db.transactionDao().getTransactionById(first.id)!!.syncStatus)
        if(restart) { db.close();open() }
        assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,outboxDispatcher(remote).once(merchant))
        assertTrue(db.saleOutboxDao().all().all { it.state=="ACKNOWLEDGED" })
        for(id in listOf(first.id,second)) {
            assertEquals("Sale $id remains pending after its receipt and queue drain","SYNCED",db.transactionDao().getTransactionById(id)!!.syncStatus)
            assertTrue(db.invoiceItemDao().getForTransactionOnce(id).all { it.syncStatus=="SYNCED" })
            assertTrue(db.stockMovementDao().forTransactionOnce(id).all { it.syncStatus=="SYNCED" })
        }
        assertTrue(db.cashBoxMovementDao().getAllOnce().all { it.syncStatus=="SYNCED" })
        assertEquals("SYNCED",db.cashBoxDao().getById("1")!!.syncStatus)
        assertEquals(200.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
        assertEquals(0.0,db.productDao().getQuantity("unit")!!,0.0)
    }
    @Test fun drainingMultipleSalesAcknowledgesEveryMatchingSaleAndLedger()=runBlocking { acknowledgeTwoSales(false) }
    @Test fun restartBetweenReceiptsDoesNotStrandEarlierSaleAsPending()=runBlocking { acknowledgeTwoSales(true) }

    @Test fun idleRestartRepairsRowsStrandedByPreviouslyAcknowledgedReceipts()=runBlocking {
        val first=original()
        val seed=db.saleOutboxDao().all().single()
        val second=SaleCommands(db).create("second-sale",merchant,sale,lines.map { it.copy(id="second-line") })
        val remote=com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport(seed)
        // Reproduce the old persisted state: real remote receipts, no local status reconciliation.
        repeat(2) {
            val event=db.saleOutboxDao().claim(merchant,"old-worker",100,1000)!!
            val result=remote.deliver(event) as com.trader.salesmanager.sales.outbox.RemoteDelivery.Applied
            assertEquals(1,db.saleOutboxDao().acknowledge(event.eventId,event.aggregateRevision,"old-worker",100,result.receipt))
        }
        db.close();open()
        val writes=remote.writes
        assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.IDLE,
            outboxDispatcher(com.trader.salesmanager.sales.outbox.OutboxTransport { error("Idle recovery must not send network requests") }).once(merchant))
        assertEquals(writes,remote.writes)
        for(id in listOf(first.id,second)) assertEquals("SYNCED",db.transactionDao().getTransactionById(id)!!.syncStatus)
        assertEquals(200.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
    }

    private suspend fun seedTwoSaleReceipts(): Pair<List<Long>,com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport> {
        val first=original();val seed=db.saleOutboxDao().all().single()
        val second=SaleCommands(db).create("second-sale",merchant,sale,lines.map { it.copy(id="second-line") })
        return listOf(first.id,second) to com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport(seed)
    }
    private fun financialSnapshot()=snapshot().filterKeys { it!="sale_outbox" }
    private fun failSecondSaleStatus(second:Long) {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER status_fault BEFORE UPDATE OF syncStatus ON transactions WHEN NEW.id=$second AND NEW.syncStatus='SYNCED' BEGIN SELECT RAISE(ABORT,'status failure'); END")
    }
    @Test fun failedDrainRollsBackAllStatusesAndReceiptThenReusesRemoteCompletion()=runBlocking {
        val (ids,remote)=seedTwoSaleReceipts()
        assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,outboxDispatcher(remote).once(merchant))
        val before=financialSnapshot();failSecondSaleStatus(ids.last())
        assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.RETRY,outboxDispatcher(remote).once(merchant))
        assertEquals(before,financialSnapshot())
        assertEquals(listOf("ACKNOWLEDGED","FAILED"),db.saleOutboxDao().all().map { it.state })
        assertNull(db.saleOutboxDao().all().last().acknowledgedAt)
        val retryAt=db.saleOutboxDao().all().last().nextAttemptAt;val writes=remote.writes
        db.close();open();db.openHelper.writableDatabase.execSQL("DROP TRIGGER status_fault")
        assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,outboxDispatcher(remote) { retryAt }.once(merchant))
        assertEquals(writes,remote.writes)
        ids.forEach { assertEquals("SYNCED",db.transactionDao().getTransactionById(it)!!.syncStatus) }
        assertEquals(200.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
    }
    @Test fun failedIdleRecoveryRollsBackEarlierStatusesWithoutLosingReceipts()=runBlocking {
        val (ids,remote)=seedTwoSaleReceipts()
        repeat(2) {
            val event=db.saleOutboxDao().claim(merchant,"legacy-worker",100,1000)!!
            val applied=remote.deliver(event) as com.trader.salesmanager.sales.outbox.RemoteDelivery.Applied
            assertEquals(1,db.saleOutboxDao().acknowledge(event.eventId,event.aggregateRevision,"legacy-worker",100,applied.receipt))
        }
        val before=snapshot();failSecondSaleStatus(ids.last())
        val noNetwork=com.trader.salesmanager.sales.outbox.OutboxTransport { error("Idle recovery must stay local") }
        try { outboxDispatcher(noNetwork).once(merchant);fail("Status fault ignored") }
        catch(expected:android.database.sqlite.SQLiteException) {}
        assertEquals(before,snapshot())
        db.close();open();db.openHelper.writableDatabase.execSQL("DROP TRIGGER status_fault")
        assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.IDLE,outboxDispatcher(noNetwork).once(merchant))
        ids.forEach { assertEquals("SYNCED",db.transactionDao().getTransactionById(it)!!.syncStatus) }
    }
    @Test fun cancellationDuringDrainRollsBackEarlierStatusesAndCanRetryAfterRestart()=runBlocking {
        val (ids,remote)=seedTwoSaleReceipts()
        assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,outboxDispatcher(remote).once(merchant))
        val before=financialSnapshot();var updates=0;var target:Job?=null
        db.close();open { sql ->
            if(sql.startsWith("UPDATE transactions SET syncStatus") && ++updates==2) target!!.cancel()
        }
        val ready=CompletableDeferred<Unit>()
        val job=launch(Dispatchers.Default) { ready.await();outboxDispatcher(remote).once(merchant) }
        target=job;ready.complete(Unit)
        withTimeout(10000) { job.join() }
        assertTrue("Cancellation point not reached",updates>=2);assertTrue(job.isCancelled)
        assertEquals(before,financialSnapshot())
        assertEquals(listOf("ACKNOWLEDGED","FAILED"),db.saleOutboxDao().all().map { it.state })
        val retryAt=db.saleOutboxDao().all().last().nextAttemptAt;val writes=remote.writes
        db.close();open()
        assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,outboxDispatcher(remote) { retryAt }.once(merchant))
        assertEquals(writes,remote.writes)
        ids.forEach { assertEquals("SYNCED",db.transactionDao().getTransactionById(it)!!.syncStatus) }
    }

    @Test fun cancelledIdleRecoveryPreservesReceiptsAndEveryLocalStatusUntilRestart()=runBlocking {
        val (ids,remote)=seedTwoSaleReceipts()
        repeat(2) {
            val event=db.saleOutboxDao().claim(merchant,"legacy-worker",100,1000)!!
            val applied=remote.deliver(event) as com.trader.salesmanager.sales.outbox.RemoteDelivery.Applied
            assertEquals(1,db.saleOutboxDao().acknowledge(event.eventId,event.aggregateRevision,"legacy-worker",100,applied.receipt))
        }
        val before=snapshot();var updates=0;var target:Job?=null
        val noNetwork=com.trader.salesmanager.sales.outbox.OutboxTransport { error("Idle recovery must stay local") }
        db.close();open { sql ->
            if(sql.startsWith("UPDATE transactions SET syncStatus") && ++updates==2) target!!.cancel()
        }
        val ready=CompletableDeferred<Unit>()
        val job=launch(Dispatchers.Default) { ready.await();outboxDispatcher(noNetwork).once(merchant) }
        target=job;ready.complete(Unit)
        withTimeout(10000) { job.join() }
        assertTrue("Cancellation point not reached",updates>=2);assertTrue(job.isCancelled)
        assertEquals(before,snapshot())
        db.close();open()
        assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.IDLE,outboxDispatcher(noNetwork).once(merchant))
        ids.forEach { assertEquals("SYNCED",db.transactionDao().getTransactionById(it)!!.syncStatus) }
        assertEquals(200.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
    }

    private fun auditSyncWrites() {
        db.openHelper.writableDatabase.execSQL("CREATE TABLE sync_write_audit(table_name TEXT,old_state TEXT,new_state TEXT)")
        for(table in listOf("transactions","invoice_items","product_units","cash_boxes","stock_movements","cash_box_movements","return_invoices"))
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER audit_$table AFTER UPDATE OF syncStatus ON $table BEGIN INSERT INTO sync_write_audit VALUES('$table',OLD.syncStatus,NEW.syncStatus); END")
    }
    private fun syncWrites():List<List<String>> = db.openHelper.writableDatabase.query("SELECT * FROM sync_write_audit ORDER BY rowid").use { c ->
        buildList { while(c.moveToNext()) add((0 until c.columnCount).map { c.getString(it) }) }
    }
    @Test fun idleRecoveryDoesNotRewriteAlreadyAcknowledgedRows()=runBlocking {
        original();val remote=com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport(db.saleOutboxDao().all().single())
        assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,outboxDispatcher(remote).once(merchant))
        val before=snapshot();auditSyncWrites()
        val noNetwork=com.trader.salesmanager.sales.outbox.OutboxTransport { error("Idle recovery must stay local") }
        repeat(2) { assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.IDLE,outboxDispatcher(noNetwork).once(merchant)) }
        assertEquals("Idle recovery rewrote synchronized rows",emptyList<List<String>>(),syncWrites())
        assertEquals(before,snapshot())
    }
    @Test fun idleRecoveryUpdatesOnlyTheMatchingUnsynchronizedRow()=runBlocking {
        val old=original();val remote=com.trader.salesmanager.sales.outbox.ProtocolMemoryTransport(db.saleOutboxDao().all().single())
        assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.ACKNOWLEDGED,outboxDispatcher(remote).once(merchant))
        db.openHelper.writableDatabase.execSQL("UPDATE invoice_items SET syncStatus='FAILED' WHERE transactionId=?",arrayOf(old.id))
        auditSyncWrites()
        val noNetwork=com.trader.salesmanager.sales.outbox.OutboxTransport { error("Idle recovery must stay local") }
        assertEquals(com.trader.salesmanager.sales.outbox.DispatchResult.IDLE,outboxDispatcher(noNetwork).once(merchant))
        assertEquals(listOf(listOf("invoice_items","FAILED","SYNCED")),syncWrites())
        assertEquals(150.0,db.cashBoxDao().getById("1")!!.currentBalance,0.0)
        assertEquals(2.0,db.productDao().getQuantity("unit")!!,0.0)
    }

}
