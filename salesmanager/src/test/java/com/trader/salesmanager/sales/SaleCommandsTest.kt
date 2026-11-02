package com.trader.salesmanager.sales

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.*
import kotlinx.coroutines.*
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
    private fun open() { db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build() }
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
        "stock_movements", "cash_boxes", "cash_box_movements", "return_invoices", "return_items", "sale_operations").associateWith { table ->
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
            transactions = listOf(TransactionEntity(customerId = 999, amount = 1.0, isPaid = false)))
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

}
