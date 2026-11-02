package com.trader.salesmanager.sales

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.trader.core.data.local.db.AppDatabase
import com.trader.core.data.local.entity.*
import com.trader.core.domain.model.*
import kotlinx.coroutines.runBlocking
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
}
